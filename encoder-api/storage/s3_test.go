package storage_test

import (
	"context"
	"errors"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	"github.com/tarcisiolzbraga/codeflix/encoder/storage"
)

const (
	bucket    = "codeflix-medias"
	accessKey = "GK31c2f218a2e44f485b94239e"
	secretKey = "4420d1b8e2e2d00a2f6b4e3e1c0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"
)

// Garage de verdade, na mesma versão do compose, e não um dublê: o que precisa ser provado aqui é
// que o SDK fala com ele, inclusive o estilo de endereço. Um dublê concordaria com qualquer coisa.
//
// --single-node e --default-bucket deixam o nó pronto com layout, bucket e chave, que é o mesmo
// que o compose faz. Sem isso o Garage sobe sem layout e recusa toda requisição.
func umGarage(t *testing.T) storage.Config {
	t.Helper()
	if testing.Short() {
		t.Skip("precisa de Docker; pulado com -short")
	}
	ctx := context.Background()

	req := testcontainers.ContainerRequest{
		Image:        "dxflrs/garage:v2.3.0",
		ExposedPorts: []string{"3900/tcp"},
		Entrypoint:   []string{"/garage"},
		Cmd:          []string{"server", "--single-node", "--default-bucket"},
		Env: map[string]string{
			"GARAGE_RPC_SECRET":         "0000000000000000000000000000000000000000000000000000000000000000",
			"GARAGE_DEFAULT_ACCESS_KEY": accessKey,
			"GARAGE_DEFAULT_SECRET_KEY": secretKey,
			"GARAGE_DEFAULT_BUCKET":     bucket,
		},
		// O Garage não sobe sem /etc/garage.toml: ele falha com "No such file or directory", que
		// lido sozinho parece imagem quebrada. É o mesmo arquivo que o compose monta, lido do
		// provisionamento — assim o teste usa a configuração de verdade, e não uma cópia que pode
		// divergir dela.
		Files: []testcontainers.ContainerFile{{
			HostFilePath:      configDoProvisionamento(t),
			ContainerFilePath: "/etc/garage.toml",
			FileMode:          0o644,
		}},
		WaitingFor: wait.ForListeningPort("3900/tcp").WithStartupTimeout(120 * time.Second),
	}
	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: req,
		Started:          true,
	})
	if err != nil {
		t.Skipf("não deu para subir o Garage: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })

	endpoint, err := container.PortEndpoint(ctx, "3900/tcp", "http")
	if err != nil {
		t.Fatalf("não deu para descobrir o endereço: %v", err)
	}
	return storage.Config{
		Endpoint:  endpoint,
		Region:    "garage",
		Bucket:    bucket,
		AccessKey: accessKey,
		SecretKey: secretKey,
	}
}

// configDoProvisionamento aponta para o garage.toml versionado, dois níveis acima: o teste e o
// compose precisam concordar, e a única forma de garantir isso é lerem o mesmo arquivo.
func configDoProvisionamento(t *testing.T) string {
	t.Helper()
	caminho, err := filepath.Abs(filepath.Join("..", "..", "provisioning", "garage", "garage.toml"))
	if err != nil {
		t.Fatalf("não deu para achar o garage.toml: %v", err)
	}
	if _, err := os.Stat(caminho); err != nil {
		t.Fatalf("o garage.toml do provisionamento devia existir em %s: %v", caminho, err)
	}
	return caminho
}

func Test_givenAnUploadedPackage_whenDownloadEachPart_thenGetThemBack(t *testing.T) {
	cfg := umGarage(t)
	store := storage.New(cfg)
	ctx := context.Background()
	origem := t.TempDir()
	if err := os.WriteFile(filepath.Join(origem, "manifest.mpd"), []byte("<MPD/>"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Join(origem, "segmentos"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(origem, "segmentos", "init.m4s"), []byte("binário"), 0o644); err != nil {
		t.Fatal(err)
	}

	err := store.UploadDir(ctx, origem, "encoded/v1-id")

	if err != nil {
		t.Fatalf("não esperava erro ao subir, veio %v", err)
	}
	destino := filepath.Join(t.TempDir(), "baixado.mpd")
	if _, err := store.Download(ctx, "encoded/v1-id/manifest.mpd", destino); err != nil {
		t.Fatalf("o manifesto devia voltar, veio %v", err)
	}
	conteudo, _ := os.ReadFile(destino)
	if string(conteudo) != "<MPD/>" {
		t.Errorf("o conteúdo voltou diferente: %q", conteudo)
	}
	// A estrutura da pasta precisa sobreviver: o manifesto aponta para os segmentos por caminho
	// relativo, e achatar a árvore quebraria o tocador.
	sub := filepath.Join(t.TempDir(), "init.m4s")
	if _, err := store.Download(ctx, "encoded/v1-id/segmentos/init.m4s", sub); err != nil {
		t.Errorf("o segmento da subpasta devia voltar, veio %v", err)
	}
}

// Arquivo apagado não melhora com nova tentativa; Garage fora, sim. O admin precisa da diferença,
// e por isso são erros distintos.
func Test_givenAKeyThatIsNotThere_whenDownload_thenSayItIsMissing(t *testing.T) {
	store := storage.New(umGarage(t))

	_, err := store.Download(context.Background(), "não/existe.mp4", filepath.Join(t.TempDir(), "x"))

	if !errors.Is(err, storage.ErrNotFound) {
		t.Errorf("esperava objeto ausente, veio %v", err)
	}
}
