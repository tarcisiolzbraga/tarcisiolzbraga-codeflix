package service_test

import (
	"context"
	"encoding/json"
	"os"
	"os/exec"
	"path/filepath"
	"testing"
	"time"

	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
	"github.com/tarcisiolzbraga/codeflix/encoder/encoding"
	"github.com/tarcisiolzbraga/codeflix/encoder/service"
	"github.com/tarcisiolzbraga/codeflix/encoder/storage"
)

// O caminho feliz com as peças de verdade: Garage em container, ffmpeg de verdade, um mp4 que
// existe. Os outros testes do serviço usam dublês para provar a ordem dos passos; este prova que
// a conversão acontece mesmo e que o pacote DASH chega ao armazenamento.
//
// Só o repositório e o publicador continuam dublês: o Postgres já tem os testes dele, e o que
// importa aqui é a resposta montada, não como ela viaja.
func Test_givenARealVideoInTheStorage_whenHandle_thenConvertAndUploadThePackage(t *testing.T) {
	if testing.Short() {
		t.Skip("precisa de Docker e ffmpeg; pulado com -short")
	}
	if _, err := exec.LookPath("ffmpeg"); err != nil {
		t.Skip("ffmpeg não está no PATH")
	}
	ctx := context.Background()
	cfg := umGarageParaOServico(t)
	armazenamento := storage.New(cfg)

	// O vídeo entra no Garage no mesmo caminho que o admin usa: <videoId>/<TYPE>.
	origem := filepath.Join(t.TempDir(), "original")
	gerarVideo(t, filepath.Join(origem, "arquivo"))
	if err := armazenamento.UploadDir(ctx, origem, "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608"); err != nil {
		t.Fatalf("não deu para plantar o vídeo: %v", err)
	}

	d := &dubles{}
	encoder := &service.Encoder{
		Storage: armazenamento, Uploader: armazenamento,
		Converter:  encoding.NewEncoder(),
		Publisher:  d,
		Repository: d,
		WorkDir:    t.TempDir(),
		Now:        func() time.Time { return agora },
		Log:        umLogSilencioso(),
	}
	aviso := `{"videoId":"9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608","type":"VIDEO",` +
		`"filePath":"9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608/arquivo","checksum":"abc123",` +
		`"occurredOn":"2026-10-09T10:00:00Z"}`

	err := encoder.Handle(ctx, []byte(aviso))

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if len(d.respostas) != 2 {
		t.Fatalf("esperava PROCESSING e COMPLETED, vieram %d respostas", len(d.respostas))
	}
	var final map[string]any
	_ = json.Unmarshal(d.respostas[1], &final)
	if final["status"] != "COMPLETED" {
		t.Fatalf("esperava COMPLETED, veio %v (%s)", final["status"], d.respostas[1])
	}
	esperado := "encoded/9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608/VIDEO"
	if final["encodedPath"] != esperado {
		t.Errorf("encodedPath veio %v", final["encodedPath"])
	}

	// O que prova a conversão: o manifesto está no Garage, no caminho que a resposta anunciou.
	baixado := filepath.Join(t.TempDir(), "manifest.mpd")
	if _, err := armazenamento.Download(ctx, esperado+"/manifest.mpd", baixado); err != nil {
		t.Fatalf("o manifesto devia estar no armazenamento: %v", err)
	}
	conteudo, _ := os.ReadFile(baixado)
	if len(conteudo) == 0 {
		t.Error("o manifesto veio vazio")
	}
}

func gerarVideo(t *testing.T, caminho string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(caminho), 0o755); err != nil {
		t.Fatal(err)
	}
	cmd := exec.Command("ffmpeg", "-hide_banner", "-loglevel", "error",
		"-f", "lavfi", "-i", "testsrc=duration=2:size=320x240:rate=15",
		"-c:v", "libx264", "-pix_fmt", "yuv420p", "-f", "mp4", caminho)
	if saida, err := cmd.CombinedOutput(); err != nil {
		t.Fatalf("não deu para gerar o vídeo: %v: %s", err, saida)
	}
}

func umGarageParaOServico(t *testing.T) storage.Config {
	t.Helper()
	ctx := context.Background()
	config, err := filepath.Abs(filepath.Join("..", "..", "provisioning", "garage", "garage.toml"))
	if err != nil {
		t.Fatal(err)
	}
	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: testcontainers.ContainerRequest{
			Image:        "dxflrs/garage:v2.3.0",
			ExposedPorts: []string{"3900/tcp"},
			Entrypoint:   []string{"/garage"},
			Cmd:          []string{"server", "--single-node", "--default-bucket"},
			Env: map[string]string{
				"GARAGE_RPC_SECRET":         "0000000000000000000000000000000000000000000000000000000000000000",
				"GARAGE_DEFAULT_ACCESS_KEY": "GK31c2f218a2e44f485b94239e",
				"GARAGE_DEFAULT_SECRET_KEY": "4420d1b8e2e2d00a2f6b4e3e1c0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b",
				"GARAGE_DEFAULT_BUCKET":     "codeflix-medias",
			},
			Files: []testcontainers.ContainerFile{{
				HostFilePath: config, ContainerFilePath: "/etc/garage.toml", FileMode: 0o644,
			}},
			WaitingFor: wait.ForListeningPort("3900/tcp").WithStartupTimeout(120 * time.Second),
		},
		Started: true,
	})
	if err != nil {
		t.Fatalf("não deu para subir o Garage: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })

	endpoint, _ := container.PortEndpoint(ctx, "3900/tcp", "http")
	return storage.Config{
		Endpoint: endpoint, Region: "garage", Bucket: "codeflix-medias",
		AccessKey: "GK31c2f218a2e44f485b94239e",
		SecretKey: "4420d1b8e2e2d00a2f6b4e3e1c0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b",
	}
}

var _ = domain.StatusCompleted
