// O processo do serviço de e-mail: liga as peças, consome a fila e para com elegância quando mandam
// parar.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"syscall"

	"github.com/tarcisiolzbraga/codeflix/mail/logging"
	"github.com/tarcisiolzbraga/codeflix/mail/mail"
	"github.com/tarcisiolzbraga/codeflix/mail/queue"
	"github.com/tarcisiolzbraga/codeflix/mail/service"
)

func main() {
	// Em ECS, como os outros serviços do monorepo: é por esses nomes de campo que a pilha de
	// observabilidade consulta.
	log := logging.New(os.Stdout, slog.LevelInfo, "mail-api")

	// SIGINT é o Ctrl+C; SIGTERM é o que o Docker manda ao derrubar o container. Sem tratar o
	// segundo, o serviço morreria no meio de um envio.
	ctx, parar := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer parar()

	if err := rodar(ctx, log); err != nil {
		log.Error("o serviço de e-mail parou com erro", "erro", err)
		os.Exit(1)
	}
	log.Info("o serviço de e-mail parou")
}

func rodar(ctx context.Context, log *slog.Logger) error {
	fila, err := queue.Connect(queue.Config{
		URL:      obrigatoria("AMQP_URL"),
		Prefetch: numero("MAIL_PREFETCH", 10),
	})
	if err != nil {
		return err
	}
	defer fila.Close()

	// Declara na subida, como a subscription-api faz do lado dela: o serviço sobe sozinho, sem
	// depender da ordem. Argumentos divergentes fazem a subida falhar, que é o que se quer.
	if err := fila.DeclareTopology(); err != nil {
		return err
	}

	quem := mail.Identity{
		FromName:    valor("MAIL_FROM_NAME", "Codeflix"),
		FromAddress: obrigatoria("MAIL_FROM_ADDRESS"),
	}
	enviador := mail.NewSMTPSender(mail.SMTPConfig{
		Addr:     obrigatoria("SMTP_ADDR"),
		Identity: quem,
	})
	servico := service.New(fila, enviador, quem, log)

	entregas, err := fila.Consume(ctx, "mail-api")
	if err != nil {
		return err
	}

	// Vários operários: mandar e-mail é esperar rede, não gastar CPU, então um processo aguenta
	// várias ao mesmo tempo. O prefetch é que limita quantas o broker entrega de uma vez.
	operarios := numero("MAIL_WORKERS", 4)
	log.Info("consumindo pedidos de e-mail", "fila", queue.ValidationQueue, "operarios", operarios)

	var grupo sync.WaitGroup
	for i := 0; i < operarios; i++ {
		grupo.Add(1)
		go func() {
			defer grupo.Done()
			for entrega := range entregas {
				// O reconhecimento é sempre o mesmo: a mensagem sai da fila daqui. Quem decide se
				// ela volta é o serviço, publicando na fila de espera — devolver com Nack faria o
				// broker reentregá-la na hora, sem o atraso da escada.
				servico.Handle(ctx, entrega.Body, entrega.Attempt)
				if err := entrega.Ack(); err != nil {
					log.Error("não deu para confirmar a mensagem", "erro", err)
				}
			}
		}()
	}
	grupo.Wait()
	return nil
}

// obrigatoria sai com erro quando a variável não existe: endereço de broker ou de SMTP faltando faz
// o serviço consumir no vazio ou enviar para lugar nenhum, e falhar na subida é melhor.
func obrigatoria(nome string) string {
	valor := os.Getenv(nome)
	if valor == "" {
		fmt.Fprintf(os.Stderr, "a variável %s é obrigatória\n", nome)
		os.Exit(1)
	}
	return valor
}

func valor(nome, padrao string) string {
	if v := os.Getenv(nome); v != "" {
		return v
	}
	return padrao
}

func numero(nome string, padrao int) int {
	v, err := strconv.Atoi(os.Getenv(nome))
	if err != nil || v <= 0 {
		return padrao
	}
	return v
}
