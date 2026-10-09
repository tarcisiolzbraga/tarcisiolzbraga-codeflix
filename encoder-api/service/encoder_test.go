package service_test

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
	"github.com/tarcisiolzbraga/codeflix/encoder/service"
	"github.com/tarcisiolzbraga/codeflix/encoder/storage"
)

var agora = time.Date(2026, 10, 9, 10, 0, 0, 0, time.UTC)

const avisoValido = `{"videoId":"9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608","type":"VIDEO",` +
	`"filePath":"9e2c1b4a/VIDEO","checksum":"abc123","occurredOn":"2026-10-09T10:00:00Z"}`

// Dublês aqui de propósito: o que precisa ser provado é a ordem dos passos e o que é respondido ao
// admin em cada caso. A infraestrutura de verdade já tem os testes dela, com container.
type dubles struct {
	baixou, converteu, subiu bool
	erroAoBaixar             error
	erroAoConverter          error
	respostas                [][]byte
	jobsGravados             int
	jobsAtualizados          int
}

func (d *dubles) Download(_ context.Context, _, destino string) (string, error) {
	if d.erroAoBaixar != nil {
		return "", d.erroAoBaixar
	}
	d.baixou = true
	_ = os.MkdirAll(filepath.Dir(destino), 0o755)
	_ = os.WriteFile(destino, []byte("vídeo"), 0o644)
	return destino, nil
}

func (d *dubles) Encode(_ context.Context, _, outputDir string) (string, error) {
	if d.erroAoConverter != nil {
		return "", d.erroAoConverter
	}
	d.converteu = true
	_ = os.MkdirAll(outputDir, 0o755)
	return filepath.Join(outputDir, "manifest.mpd"), nil
}

func (d *dubles) UploadDir(_ context.Context, _, _ string) error { d.subiu = true; return nil }

func (d *dubles) Publish(_ context.Context, payload []byte) error {
	d.respostas = append(d.respostas, payload)
	return nil
}

func (d *dubles) SaveMedia(_ context.Context, m *domain.Media) (*domain.Media, error) { return m, nil }
func (d *dubles) SaveJob(_ context.Context, _ *domain.Job) error                      { d.jobsGravados++; return nil }
func (d *dubles) UpdateJob(_ context.Context, _ *domain.Job) error                    { d.jobsAtualizados++; return nil }

func umEncoder(t *testing.T, d *dubles) *service.Encoder {
	t.Helper()
	return &service.Encoder{
		Storage: d, Uploader: d, Converter: d, Publisher: d, Repository: d,
		WorkDir: t.TempDir(),
		Now:     func() time.Time { return agora },
		Log:     umLogSilencioso(),
	}
}

func umLogSilencioso() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}

func statusDe(t *testing.T, payload []byte) string {
	t.Helper()
	var m map[string]any
	if err := json.Unmarshal(payload, &m); err != nil {
		t.Fatalf("a resposta devia ser JSON: %v", err)
	}
	return m["status"].(string)
}

// Duas respostas, nesta ordem: PROCESSING tira a mídia de PENDING no admin antes do trabalho
// começar, e COMPLETED diz onde a saída ficou. Invertê-las esconderia a conversão de quem olha.
func Test_givenAValidNotice_whenHandle_thenAnswerProcessingThenCompleted(t *testing.T) {
	d := &dubles{}

	err := umEncoder(t, d).Handle(context.Background(), []byte(avisoValido))

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if !d.baixou || !d.converteu || !d.subiu {
		t.Errorf("os três passos deviam acontecer: baixou=%v converteu=%v subiu=%v", d.baixou, d.converteu, d.subiu)
	}
	if len(d.respostas) != 2 {
		t.Fatalf("esperava duas respostas, vieram %d", len(d.respostas))
	}
	if statusDe(t, d.respostas[0]) != "PROCESSING" {
		t.Errorf("a primeira devia ser PROCESSING, veio %s", statusDe(t, d.respostas[0]))
	}
	if statusDe(t, d.respostas[1]) != "COMPLETED" {
		t.Errorf("a segunda devia ser COMPLETED, veio %s", statusDe(t, d.respostas[1]))
	}
}

// Conversão que falha não deixa a mídia parada: o admin recebe ERROR com o motivo, e o trabalho
// fica registrado como falho.
func Test_givenAConversionThatFails_whenHandle_thenAnswerErrorWithTheReason(t *testing.T) {
	d := &dubles{erroAoConverter: errors.New("codec não suportado")}

	err := umEncoder(t, d).Handle(context.Background(), []byte(avisoValido))

	if err != nil {
		t.Fatalf("falha de conversão vira resposta, não erro: %v", err)
	}
	if len(d.respostas) != 2 {
		t.Fatalf("esperava duas respostas, vieram %d", len(d.respostas))
	}
	if statusDe(t, d.respostas[1]) != "ERROR" {
		t.Errorf("a segunda devia ser ERROR, veio %s", statusDe(t, d.respostas[1]))
	}
	if d.subiu {
		t.Error("não devia subir nada depois de a conversão falhar")
	}
	if d.jobsAtualizados != 1 {
		t.Errorf("o trabalho devia ser atualizado como falho, veio %d", d.jobsAtualizados)
	}
}

// Aviso ilegível é descartado sem resposta: sem videoId, type e checksum confiáveis, o admin não
// teria como saber de qual envio a resposta fala.
func Test_givenAnUnreadableNotice_whenHandle_thenDiscardWithoutAnswering(t *testing.T) {
	d := &dubles{}

	err := umEncoder(t, d).Handle(context.Background(), []byte("não é json"))

	if err != nil {
		t.Errorf("aviso ilegível não volta para a fila, veio %v", err)
	}
	if len(d.respostas) != 0 {
		t.Errorf("não há a quem responder, vieram %d respostas", len(d.respostas))
	}
	if d.jobsGravados != 0 {
		t.Error("não devia criar trabalho para aviso que não dá para entender")
	}
}

func Test_givenAFileThatIsNotInTheStorage_whenHandle_thenAnswerError(t *testing.T) {
	d := &dubles{erroAoBaixar: storage.ErrNotFound}

	err := umEncoder(t, d).Handle(context.Background(), []byte(avisoValido))

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if statusDe(t, d.respostas[len(d.respostas)-1]) != "ERROR" {
		t.Error("arquivo ausente devia virar ERROR para o admin")
	}
}

// Devolver à fila só serve ao que pode dar certo numa segunda tentativa. Arquivo que não existe
// não vai aparecer, e a mensagem ficaria rodando para sempre.
func Test_givenDifferentFailures_whenAskIfTransient_thenTellThemApart(t *testing.T) {
	casos := []struct {
		nome       string
		err        error
		passageiro bool
	}{
		{"armazenamento fora", errors.New("connection refused"), true},
		{"objeto ausente", storage.ErrNotFound, false},
		{"tipo desconhecido", domain.ErrUnknownMediaType, false},
		{"sem erro", nil, false},
	}
	for _, caso := range casos {
		t.Run(caso.nome, func(t *testing.T) {
			actual := service.EhPassageiro(caso.err)

			if actual != caso.passageiro {
				t.Errorf("esperava %v, veio %v", caso.passageiro, actual)
			}
		})
	}
}

// Desistir calado deixaria a mídia em PROCESSING para sempre: o admin só sai desse estado quando o
// encoder responde.
func Test_givenExhaustedAttempts_whenReportFailure_thenTellTheAdmin(t *testing.T) {
	d := &dubles{}

	err := umEncoder(t, d).ReportFailure(context.Background(), []byte(avisoValido), "tentativas esgotadas")

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if len(d.respostas) != 1 {
		t.Fatalf("esperava uma resposta, vieram %d", len(d.respostas))
	}
	var m map[string]any
	_ = json.Unmarshal(d.respostas[0], &m)
	if m["status"] != "ERROR" {
		t.Errorf("esperava ERROR, veio %v", m["status"])
	}
	if m["message"] != "tentativas esgotadas" {
		t.Errorf("o motivo devia viajar junto, veio %v", m["message"])
	}
	// O checksum é o que permite ao admin saber de qual envio esta desistência fala.
	if m["checksum"] != "abc123" {
		t.Errorf("o checksum devia voltar, veio %v", m["checksum"])
	}
}

func Test_givenAnUnreadableNotice_whenReportFailure_thenSayThereIsNobodyToTell(t *testing.T) {
	d := &dubles{}

	err := umEncoder(t, d).ReportFailure(context.Background(), []byte("lixo"), "qualquer")

	if err == nil {
		t.Error("sem videoId e checksum não há a quem responder")
	}
	if len(d.respostas) != 0 {
		t.Error("não devia publicar resposta nenhuma")
	}
}

// A escada de tentativas tinha teste nas peças — a fila devolve depois do atraso, o serviço avisa
// o admin —, mas a decisão que as combina vivia dentro do main e não era exercitada por nada.
func Test_givenAFailureAndAnAttemptCount_whenDecide_thenChooseWhatToDo(t *testing.T) {
	passageiro := errors.New("connection refused")
	casos := []struct {
		nome      string
		err       error
		tentativa int
		max       int
		esperado  service.Decisao
	}{
		{"sucesso", nil, 0, 5, service.Confirmar},
		{"passageiro com folga", passageiro, 0, 5, service.Reagendar},
		{"passageiro na última", passageiro, 4, 5, service.Reagendar},
		{"passageiro esgotado", passageiro, 5, 5, service.Desistir},
		{"passageiro além do teto", passageiro, 9, 5, service.Desistir},
		{"definitivo não insiste", storage.ErrNotFound, 0, 5, service.Descartar},
		{"tipo desconhecido não insiste", domain.ErrUnknownMediaType, 0, 5, service.Descartar},
	}
	for _, caso := range casos {
		t.Run(caso.nome, func(t *testing.T) {
			actual := service.Decidir(caso.err, caso.tentativa, caso.max)

			if actual != caso.esperado {
				t.Errorf("esperava %v, veio %v", caso.esperado, actual)
			}
		})
	}
}

// O atraso dobra e tem teto. O teto importa: sem ele a espera cresceria até valores absurdos, e o
// deslocamento acabaria estourando o inteiro.
func Test_givenAnAttemptNumber_whenAskTheDelay_thenDoubleUpToTheCap(t *testing.T) {
	esperados := map[int]time.Duration{
		1: time.Second, 2: 2 * time.Second, 3: 4 * time.Second,
		6: 32 * time.Second, 7: time.Minute, 60: time.Minute,
	}
	for tentativa, esperado := range esperados {
		actual := service.AtrasoDe(tentativa)

		if actual != esperado {
			t.Errorf("tentativa %d: esperava %v, veio %v", tentativa, esperado, actual)
		}
	}
}
