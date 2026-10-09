// O processo do encoder: liga as peças, consome a fila e para com elegância quando mandam parar.
package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"syscall"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/encoding"
	"github.com/tarcisiolzbraga/codeflix/encoder/persistence"
	"github.com/tarcisiolzbraga/codeflix/encoder/queue"
	"github.com/tarcisiolzbraga/codeflix/encoder/service"
	"github.com/tarcisiolzbraga/codeflix/encoder/storage"
)

func main() {
	log := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))

	// SIGINT e SIGTERM: o primeiro é o Ctrl+C, o segundo é o que o Docker manda ao derrubar o
	// container. Sem tratar o segundo, o encoder morreria no meio de uma conversão.
	ctx, parar := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer parar()

	if err := rodar(ctx, log); err != nil {
		log.Error("o encoder parou com erro", "erro", err)
		os.Exit(1)
	}
	log.Info("o encoder parou")
}

func rodar(ctx context.Context, log *slog.Logger) error {
	db, err := persistence.Connect(ctx, obrigatoria("DATABASE_URL"))
	if err != nil {
		return err
	}
	defer db.Close()

	// A migration roda na subida, como o Flyway dos serviços Java: o processo não começa a
	// trabalhar contra um esquema que não conhece.
	if err := db.Migrate(ctx); err != nil {
		return err
	}

	fila, err := queue.Connect(queue.Config{
		URL:      obrigatoria("AMQP_URL"),
		Prefetch: inteiro("ENCODER_PREFETCH", 1),
	})
	if err != nil {
		return err
	}
	defer fila.Close()

	if err := fila.DeclareTopology(); err != nil {
		return err
	}

	// O mesmo Store serve de Downloader e de Uploader: são dois papéis do mesmo armazenamento, e o
	// serviço os pede separados só para poder ser testado com dublês diferentes.
	armazenamento := storage.New(storage.Config{
		Endpoint:  obrigatoria("STORAGE_ENDPOINT"),
		Region:    comPadrao("STORAGE_REGION", "garage"),
		Bucket:    obrigatoria("STORAGE_BUCKET"),
		AccessKey: obrigatoria("STORAGE_ACCESS_KEY"),
		SecretKey: obrigatoria("STORAGE_SECRET_KEY"),
	})

	encoder := &service.Encoder{
		Storage:    armazenamento,
		Uploader:   armazenamento,
		Converter:  encoding.NewEncoder(),
		Publisher:  fila,
		Repository: persistence.NewRepository(db),
		WorkDir:    comPadrao("ENCODER_WORK_DIR", os.TempDir()),
		Now:        time.Now,
		Log:        log,
	}

	entregas, err := fila.Consume(ctx, "encoder")
	if err != nil {
		return err
	}

	operarios := inteiro("ENCODER_WORKERS", 2)
	log.Info("encoder no ar", "operários", operarios)

	var grupo sync.WaitGroup
	for i := 0; i < operarios; i++ {
		grupo.Add(1)
		go func() {
			defer grupo.Done()
			for entrega := range entregas {
				processar(ctx, encoder, entrega, log)
			}
		}()
	}

	// Espera os operários terminarem o que já pegaram antes de devolver: é o que faz a parada ser
	// elegante em vez de abrupta.
	grupo.Wait()
	return nil
}

func processar(ctx context.Context, encoder *service.Encoder, entrega queue.Delivery, log *slog.Logger) {
	err := encoder.Handle(ctx, entrega.Body)
	switch {
	case err == nil:
		if err := entrega.Ack(); err != nil {
			log.Error("não deu para confirmar a mensagem", "erro", err)
		}
	case service.EhPassageiro(err):
		log.Warn("falha passageira; a mensagem volta para a fila", "erro", err)
		if err := entrega.Requeue(); err != nil {
			log.Error("não deu para devolver a mensagem", "erro", err)
		}
	default:
		log.Error("falha definitiva; a mensagem é descartada", "erro", err)
		if err := entrega.Discard(); err != nil {
			log.Error("não deu para descartar a mensagem", "erro", err)
		}
	}
}

// obrigatoria derruba a subida quando falta configuração, em vez de deixar o encoder rodar
// apontando para lugar nenhum. É a mesma escolha dos serviços Java fora do perfil de
// desenvolvimento: variável faltando não tem valor padrão.
func obrigatoria(nome string) string {
	valor := os.Getenv(nome)
	if valor == "" {
		slog.Error("variável obrigatória não definida", "nome", nome)
		os.Exit(1)
	}
	return valor
}

func comPadrao(nome, padrao string) string {
	if valor := os.Getenv(nome); valor != "" {
		return valor
	}
	return padrao
}

func inteiro(nome string, padrao int) int {
	valor, err := strconv.Atoi(os.Getenv(nome))
	if err != nil || valor <= 0 {
		return padrao
	}
	return valor
}
