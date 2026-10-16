package persistence_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
	"github.com/tarcisiolzbraga/codeflix/encoder/persistence"
)

var agora = time.Date(2026, 10, 9, 10, 0, 0, 0, time.UTC)

// Postgres de verdade, e não SQLite em memória como o curso usa nos testes dele. É a mesma regra
// dos serviços Java: banco de teste igual ao de produção verifica de fato os tipos, o ON CONFLICT
// e o índice único — nada disso o SQLite exercitaria do mesmo jeito.
func umPostgres(t *testing.T) *persistence.DB {
	t.Helper()
	if testing.Short() {
		t.Skip("precisa de Docker; pulado com -short")
	}
	ctx := context.Background()

	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: testcontainers.ContainerRequest{
			Image:        "postgres:17-alpine",
			ExposedPorts: []string{"5432/tcp"},
			Env: map[string]string{
				"POSTGRES_USER":     "encoder",
				"POSTGRES_PASSWORD": "encoder",
				"POSTGRES_DB":       "encoder",
			},
			WaitingFor: wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).WithStartupTimeout(120 * time.Second),
		},
		Started: true,
	})
	if err != nil {
		t.Fatalf("não deu para subir o Postgres: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })

	host, _ := container.Host(ctx)
	porta, _ := container.MappedPort(ctx, "5432/tcp")
	url := "postgres://encoder:encoder@" + host + ":" + porta.Port() + "/encoder?sslmode=disable"

	db, err := persistence.Connect(ctx, url)
	if err != nil {
		t.Fatalf("não deu para conectar: %v", err)
	}
	t.Cleanup(db.Close)
	if err := db.Migrate(ctx); err != nil {
		t.Fatalf("a migration devia aplicar: %v", err)
	}
	return db
}

func umaMedia(t *testing.T, checksum string) *domain.Media {
	t.Helper()
	media, err := domain.NewMedia("9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608", domain.MediaTypeVideo,
		checksum, "9e2c1b4a/VIDEO", agora)
	if err != nil {
		t.Fatalf("a mídia devia nascer válida: %v", err)
	}
	return media
}

func Test_givenAMedia_whenSaveAndFetch_thenGetItBackIntact(t *testing.T) {
	repo := persistence.NewRepository(umPostgres(t))
	ctx := context.Background()
	media := umaMedia(t, "abc123")

	gravada, err := repo.SaveMedia(ctx, media)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	lida, err := repo.MediaOfUpload(ctx, media.VideoID, media.Type, media.Checksum)
	if err != nil {
		t.Fatalf("a mídia devia ser encontrada, veio %v", err)
	}
	if lida.ID != gravada.ID || lida.FilePath != media.FilePath || lida.Type != domain.MediaTypeVideo {
		t.Errorf("voltou diferente: %+v", lida)
	}
}

// A entrega da fila é ao menos uma vez: o relay da saída do admin pode repetir a mesma mensagem.
// Gravar duas mídias para o mesmo envio criaria dois trabalhos para a mesma conversão.
func Test_givenTheSameUploadTwice_whenSave_thenKeepOneAndReturnIt(t *testing.T) {
	repo := persistence.NewRepository(umPostgres(t))
	ctx := context.Background()
	primeira, _ := repo.SaveMedia(ctx, umaMedia(t, "mesmo-checksum"))

	segunda, err := repo.SaveMedia(ctx, umaMedia(t, "mesmo-checksum"))

	if err != nil {
		t.Fatalf("a segunda gravação devia passar, veio %v", err)
	}
	if segunda.ID != primeira.ID {
		t.Errorf("devia devolver a que já existe: %s vs %s", segunda.ID, primeira.ID)
	}
}

// Checksum diferente é envio diferente: o usuário trocou o arquivo, e isso é outra conversão.
func Test_givenADifferentChecksum_whenSave_thenItIsAnotherMedia(t *testing.T) {
	repo := persistence.NewRepository(umPostgres(t))
	ctx := context.Background()
	primeira, _ := repo.SaveMedia(ctx, umaMedia(t, "checksum-antigo"))

	segunda, err := repo.SaveMedia(ctx, umaMedia(t, "checksum-novo"))

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if segunda.ID == primeira.ID {
		t.Error("envios diferentes precisam de mídias diferentes")
	}
}

func Test_givenAJob_whenCompleteAndUpdate_thenTheLastOneReflectsIt(t *testing.T) {
	repo := persistence.NewRepository(umPostgres(t))
	ctx := context.Background()
	media, _ := repo.SaveMedia(ctx, umaMedia(t, "abc123"))
	job, _ := domain.NewJob("encoded/v1", media, agora)
	if err := repo.SaveJob(ctx, job); err != nil {
		t.Fatalf("o trabalho devia ser gravado: %v", err)
	}
	_ = job.Complete(agora.Add(time.Minute))

	err := repo.UpdateJob(ctx, job)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	ultimo, err := repo.LastJobOfMedia(ctx, media)
	if err != nil {
		t.Fatalf("devia achar o trabalho, veio %v", err)
	}
	if ultimo.Status != domain.StatusCompleted {
		t.Errorf("esperava COMPLETED, veio %q", ultimo.Status)
	}
}

func Test_givenAFailedJob_whenRead_thenTheReasonSurvives(t *testing.T) {
	repo := persistence.NewRepository(umPostgres(t))
	ctx := context.Background()
	media, _ := repo.SaveMedia(ctx, umaMedia(t, "abc123"))
	job, _ := domain.NewJob("encoded/v1", media, agora)
	_ = repo.SaveJob(ctx, job)
	_ = job.Fail("codec não suportado", agora.Add(time.Minute))
	_ = repo.UpdateJob(ctx, job)

	ultimo, err := repo.LastJobOfMedia(ctx, media)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if ultimo.Error != "codec não suportado" {
		t.Errorf("o motivo devia sobreviver, veio %q", ultimo.Error)
	}
}

func Test_givenAnUnknownUpload_whenFetch_thenSayItIsMissing(t *testing.T) {
	repo := persistence.NewRepository(umPostgres(t))

	_, err := repo.MediaOfUpload(context.Background(), "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608",
		domain.MediaTypeTrailer, "nunca-visto")

	if !errors.Is(err, persistence.ErrNotFound) {
		t.Errorf("esperava não encontrado, veio %v", err)
	}
}

// Subir duas vezes não pode tentar criar as tabelas de novo: é o papel da tabela de controle, que
// faz aqui o que o flyway_schema_history faz nos serviços Java.
func Test_givenAnAlreadyMigratedDatabase_whenMigrateAgain_thenDoNothing(t *testing.T) {
	db := umPostgres(t)

	err := db.Migrate(context.Background())

	if err != nil {
		t.Errorf("migrar de novo devia ser inócuo, veio %v", err)
	}
}
