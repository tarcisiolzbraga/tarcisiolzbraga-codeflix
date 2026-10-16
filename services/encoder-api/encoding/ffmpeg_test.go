package encoding_test

import (
	"context"
	"errors"
	"os"
	"os/exec"
	"path/filepath"
	"testing"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/encoding"
)

// O vídeo de entrada é gerado na hora pelo próprio ffmpeg, e não guardado no repositório: binário
// versionado envelhece e ninguém sabe de onde veio.
func umVideoDeTeste(t *testing.T, dir string) string {
	t.Helper()
	caminho := filepath.Join(dir, "entrada.mp4")
	cmd := exec.Command("ffmpeg", "-hide_banner", "-loglevel", "error",
		"-f", "lavfi", "-i", "testsrc=duration=2:size=320x240:rate=15",
		"-c:v", "libx264", "-pix_fmt", "yuv420p", caminho)
	if saida, err := cmd.CombinedOutput(); err != nil {
		t.Fatalf("não deu para gerar o vídeo de teste: %v: %s", err, saida)
	}
	return caminho
}

func exigeFFmpeg(t *testing.T) {
	t.Helper()
	if _, err := exec.LookPath("ffmpeg"); err != nil {
		t.Skip("ffmpeg não está no PATH")
	}
}

func Test_givenAnMp4_whenEncode_thenProduceADashPackage(t *testing.T) {
	exigeFFmpeg(t)
	dir := t.TempDir()
	entrada := umVideoDeTeste(t, dir)
	saidaDir := filepath.Join(dir, "saida")
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	manifesto, err := encoding.NewEncoder().Encode(ctx, entrada, saidaDir)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if filepath.Base(manifesto) != "manifest.mpd" {
		t.Errorf("esperava o manifesto, veio %q", manifesto)
	}
	if _, err := os.Stat(manifesto); err != nil {
		t.Errorf("o manifesto devia existir: %v", err)
	}
	arquivos, _ := os.ReadDir(saidaDir)
	if len(arquivos) < 2 {
		t.Errorf("esperava manifesto e ao menos um segmento, veio %d arquivo(s)", len(arquivos))
	}
}

// O muxer de DASH do ffmpeg não cria a pasta de saída: ele falha com "No such file or directory",
// que lido sozinho parece arquivo de entrada faltando. Criar a pasta é responsabilidade nossa.
func Test_givenAnOutputDirThatDoesNotExist_whenEncode_thenCreateIt(t *testing.T) {
	exigeFFmpeg(t)
	dir := t.TempDir()
	entrada := umVideoDeTeste(t, dir)
	fundo := filepath.Join(dir, "a", "b", "c")
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	_, err := encoding.NewEncoder().Encode(ctx, entrada, fundo)

	if err != nil {
		t.Fatalf("a pasta devia ser criada, veio %v", err)
	}
}

func Test_givenAMissingInput_whenEncode_thenFail(t *testing.T) {
	exigeFFmpeg(t)
	ctx := context.Background()

	_, err := encoding.NewEncoder().Encode(ctx, filepath.Join(t.TempDir(), "não-existe.mp4"), t.TempDir())

	if !errors.Is(err, encoding.ErrConversion) {
		t.Errorf("esperava falha de conversão, veio %v", err)
	}
}

// Arquivo que existe mas não é vídeo: o erro tem de trazer o que o ffmpeg disse, senão o log fica
// com "exit status 1" e ninguém descobre o motivo.
func Test_givenAFileThatIsNotAVideo_whenEncode_thenFailSayingWhy(t *testing.T) {
	exigeFFmpeg(t)
	dir := t.TempDir()
	lixo := filepath.Join(dir, "lixo.mp4")
	if err := os.WriteFile(lixo, []byte("isto não é um vídeo"), 0o644); err != nil {
		t.Fatal(err)
	}

	_, err := encoding.NewEncoder().Encode(context.Background(), lixo, filepath.Join(dir, "saida"))

	if !errors.Is(err, encoding.ErrConversion) {
		t.Fatalf("esperava falha de conversão, veio %v", err)
	}
	if len(err.Error()) < 40 {
		t.Errorf("o erro devia trazer o motivo do ffmpeg, veio %q", err)
	}
}

func Test_givenNoFFmpeg_whenEncode_thenSayItIsMissing(t *testing.T) {
	encoder := &encoding.Encoder{Binary: "ffmpeg-que-nao-existe", SegmentSeconds: 4}

	_, err := encoder.Encode(context.Background(), "qualquer.mp4", t.TempDir())

	if !errors.Is(err, encoding.ErrFFmpegMissing) {
		t.Errorf("esperava ferramenta ausente, veio %v", err)
	}
}
