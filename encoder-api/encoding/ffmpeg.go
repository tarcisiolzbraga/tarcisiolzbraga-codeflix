// Package encoding faz a conversão: recebe um mp4 e devolve uma pasta empacotada em MPEG-DASH,
// pronta para um tocador pedir por partes.
//
// Com ffmpeg, e não com o Bento4 do curso. Lá são duas ferramentas — mp4fragment fragmenta e
// mp4dash empacota —, e o ffmpeg faz as duas coisas num passo só. Uma dependência em vez de duas,
// e o formato de saída é o mesmo: manifesto .mpd, segmento de inicialização e os chunks.
package encoding

import (
	"context"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

var (
	// ErrFFmpegMissing separa "a ferramenta não está aí" de "o vídeo é ruim": o primeiro é problema de
	// ambiente e não adianta tentar de novo com outro arquivo.
	ErrFFmpegMissing = errors.New("ffmpeg não encontrado no PATH")
	ErrConversion    = errors.New("a conversão falhou")
)

const manifestName = "manifest.mpd"

// Encoder converte um arquivo local em uma pasta DASH.
type Encoder struct {
	// Binary é o executável; fica configurável para o teste e para a imagem poder usar outro
	// caminho sem recompilar.
	Binary string
	// SegmentSeconds é o tamanho de cada pedaço. Quanto menor, mais rápido o tocador começa e mais
	// arquivos a pasta tem.
	SegmentSeconds int
}

func NewEncoder() *Encoder {
	return &Encoder{Binary: "ffmpeg", SegmentSeconds: 4}
}

// Encode empacota source dentro de outputDir e devolve o caminho do manifesto.
//
// A pasta é criada aqui porque o muxer de DASH do ffmpeg não a cria: ele falha com "No such file
// or directory", que lido sozinho parece arquivo de entrada faltando.
func (e *Encoder) Encode(ctx context.Context, source, outputDir string) (string, error) {
	if _, err := exec.LookPath(e.Binary); err != nil {
		return "", fmt.Errorf("%w: %s", ErrFFmpegMissing, e.Binary)
	}
	if _, err := os.Stat(source); err != nil {
		return "", fmt.Errorf("%w: arquivo de entrada: %w", ErrConversion, err)
	}
	if err := os.MkdirAll(outputDir, 0o755); err != nil {
		return "", fmt.Errorf("%w: pasta de saída: %w", ErrConversion, err)
	}

	manifest := filepath.Join(outputDir, manifestName)
	cmd := exec.CommandContext(ctx, e.Binary,
		"-hide_banner", "-loglevel", "error",
		"-i", source,
		"-c", "copy",
		"-f", "dash",
		"-seg_duration", fmt.Sprint(e.SegmentSeconds),
		manifest,
	)

	// CombinedOutput porque o ffmpeg escreve o motivo real no stderr: sem ele o erro seria só
	// "exit status 1", que não diz nada a quem for ler o log.
	saida, err := cmd.CombinedOutput()
	if err != nil {
		return "", fmt.Errorf("%w: %w: %s", ErrConversion, err, strings.TrimSpace(string(saida)))
	}
	if _, err := os.Stat(manifest); err != nil {
		return "", fmt.Errorf("%w: o manifesto não foi gerado", ErrConversion)
	}
	return manifest, nil
}
