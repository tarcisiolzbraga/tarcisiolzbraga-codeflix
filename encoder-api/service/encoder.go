// Package service costura as peças: recebe o aviso, converte e responde.
//
// A ordem dos passos é a mesma do curso — baixar, converter, subir, responder —, mas as respostas
// são as do contrato daqui: PROCESSING assim que o trabalho começa, e depois COMPLETED ou ERROR.
package service

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
	"github.com/tarcisiolzbraga/codeflix/encoder/messaging"
	"github.com/tarcisiolzbraga/codeflix/encoder/storage"
)

// As dependências entram por interface para o serviço ser testável sem subir nada. As
// implementações de verdade estão em storage, encoding, queue e persistence.
type Downloader interface {
	Download(ctx context.Context, key, destino string) (string, error)
}

type Uploader interface {
	UploadDir(ctx context.Context, dir, prefix string) error
}

type Converter interface {
	Encode(ctx context.Context, source, outputDir string) (string, error)
}

type Publisher interface {
	Publish(ctx context.Context, payload []byte) error
}

type Repository interface {
	SaveMedia(ctx context.Context, media *domain.Media) (*domain.Media, error)
	SaveJob(ctx context.Context, job *domain.Job) error
	UpdateJob(ctx context.Context, job *domain.Job) error
}

type Encoder struct {
	Storage    Downloader
	Uploader   Uploader
	Converter  Converter
	Publisher  Publisher
	Repository Repository
	// WorkDir é onde o arquivo baixado e a saída ficam enquanto o trabalho corre. Some no fim: o
	// que importa guardar já está no Garage.
	WorkDir string
	Now     func() time.Time
	Log     *slog.Logger
}

// Handle processa um aviso. Devolve erro só quando tentar de novo tem chance de dar certo; o que
// não tem — aviso ilegível, tipo desconhecido — vira resposta de erro ao admin e nil aqui, porque
// devolver à fila faria a mensagem voltar para sempre.
func (e *Encoder) Handle(ctx context.Context, payload []byte) error {
	media, err := messaging.ParseVideoMediaCreated(payload, e.Now())
	if err != nil {
		// Sem mídia não há a quem responder: o admin precisa de videoId, type e checksum para
		// saber de qual envio a resposta fala, e nenhum deles é confiável aqui.
		e.Log.Error("aviso ilegível, descartado", "erro", err, "payload", string(payload))
		return nil
	}

	media, err = e.Repository.SaveMedia(ctx, media)
	if err != nil {
		return fmt.Errorf("gravar mídia: %w", err)
	}

	destino := filepath.Join("encoded", media.VideoID, media.Type.String())
	job, err := domain.NewJob(destino, media, e.Now())
	if err != nil {
		return fmt.Errorf("criar trabalho: %w", err)
	}
	if err := e.Repository.SaveJob(ctx, job); err != nil {
		return fmt.Errorf("gravar trabalho: %w", err)
	}

	// PROCESSING vai antes do trabalho começar: é ela que tira a mídia de PENDING no admin e diz
	// que alguém pegou. Mandá-la depois esconderia a conversão inteira de quem está olhando.
	if err := e.responder(ctx, job); err != nil {
		return err
	}

	if err := e.converter(ctx, job); err != nil {
		e.Log.Error("conversão falhou", "videoId", media.VideoID, "tipo", media.Type, "erro", err)
		if erroDeEstado := job.Fail(err.Error(), e.Now()); erroDeEstado != nil {
			return erroDeEstado
		}
	} else if erroDeEstado := job.Complete(e.Now()); erroDeEstado != nil {
		return erroDeEstado
	}

	if err := e.Repository.UpdateJob(ctx, job); err != nil {
		return fmt.Errorf("atualizar trabalho: %w", err)
	}
	return e.responder(ctx, job)
}

// ReportFailure avisa o admin de que esta mídia não vai converter.
//
// Existe para quando as tentativas se esgotam: o admin só tira a mídia de PROCESSING quando o
// encoder responde, então desistir sem avisar a deixaria presa para sempre. Monta a resposta a
// partir do aviso original porque, a essa altura, não há trabalho em memória.
func (e *Encoder) ReportFailure(ctx context.Context, payload []byte, motivo string) error {
	media, err := messaging.ParseVideoMediaCreated(payload, e.Now())
	if err != nil {
		// Se nem dá para ler o aviso, não há a quem responder: o admin precisa de videoId, type e
		// checksum para saber de qual envio a resposta fala.
		return fmt.Errorf("aviso ilegível, sem a quem responder: %w", err)
	}

	job, err := domain.NewJob("", media, e.Now())
	if err != nil {
		// O caminho de saída é vazio porque não houve saída; o construtor o exige, então o erro é
		// montado à mão.
		job = &domain.Job{Media: media, Status: domain.StatusError, Error: motivo, CreatedAt: e.Now()}
	} else if err := job.Fail(motivo, e.Now()); err != nil {
		return err
	}
	job.Status = domain.StatusError
	job.Error = motivo

	return e.responder(ctx, job)
}

// converter faz o trabalho em si, numa pasta temporária que some no fim.
func (e *Encoder) converter(ctx context.Context, job *domain.Job) error {
	trabalho, err := os.MkdirTemp(e.WorkDir, "job-")
	if err != nil {
		return fmt.Errorf("pasta de trabalho: %w", err)
	}
	defer os.RemoveAll(trabalho)

	origem := filepath.Join(trabalho, "original.mp4")
	if _, err := e.Storage.Download(ctx, job.Media.FilePath, origem); err != nil {
		return fmt.Errorf("baixar: %w", err)
	}

	saida := filepath.Join(trabalho, "saida")
	if _, err := e.Converter.Encode(ctx, origem, saida); err != nil {
		return fmt.Errorf("converter: %w", err)
	}

	if err := e.Uploader.UploadDir(ctx, saida, job.OutputBucketPath); err != nil {
		return fmt.Errorf("subir: %w", err)
	}
	return nil
}

func (e *Encoder) responder(ctx context.Context, job *domain.Job) error {
	payload, err := messaging.Encode(job)
	if err != nil {
		return fmt.Errorf("montar resposta: %w", err)
	}
	if err := e.Publisher.Publish(ctx, payload); err != nil {
		return fmt.Errorf("publicar resposta: %w", err)
	}
	return nil
}

// EhPassageiro diz se vale devolver a mensagem à fila para outra tentativa. Armazenamento fora do
// ar volta; objeto que não existe, não — o arquivo não vai aparecer numa segunda tentativa, e a
// mensagem ficaria rodando para sempre.
func EhPassageiro(err error) bool {
	if err == nil {
		return false
	}
	return !errors.Is(err, storage.ErrNotFound) && !errors.Is(err, domain.ErrUnknownMediaType)
}

// Decisao é o que fazer com uma mensagem depois de processada.
type Decisao int

const (
	// Confirmar: deu certo, ou falhou de um jeito que não melhora. A mensagem sai da fila.
	Confirmar Decisao = iota
	// Reagendar: falha passageira com tentativas sobrando. Volta depois do atraso.
	Reagendar
	// Desistir: tentativas esgotadas. Avisa o admin e tira da fila.
	Desistir
	// Descartar: falha definitiva de processamento, sem a quem responder.
	Descartar
)

// Decidir separa a política da fiação. Estava dentro do main, onde não havia como exercitá-la: a
// escada de tentativas só tinha teste nas peças, e nunca na decisão que as combina.
func Decidir(err error, tentativaAtual, maxTentativas int) Decisao {
	switch {
	case err == nil:
		return Confirmar
	case !EhPassageiro(err):
		return Descartar
	case tentativaAtual+1 > maxTentativas:
		return Desistir
	default:
		return Reagendar
	}
}

// AtrasoDe dobra a cada tentativa, com teto. É a mesma forma da escada da videos-api: cresce rápido
// o bastante para cobrir uma indisponibilidade, e o teto evita esperas absurdas.
func AtrasoDe(tentativa int) time.Duration {
	if tentativa < 1 {
		tentativa = 1
	}
	// O deslocamento estoura o int64 bem antes disto, e o teto já cobre qualquer valor grande.
	if tentativa > 20 {
		return time.Minute
	}
	atraso := time.Second << (tentativa - 1)
	if atraso > time.Minute {
		return time.Minute
	}
	return atraso
}
