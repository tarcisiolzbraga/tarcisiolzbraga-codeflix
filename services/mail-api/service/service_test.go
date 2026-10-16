package service_test

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"strings"
	"testing"
	"time"

	"github.com/tarcisiolzbraga/codeflix/mail/mail"
	"github.com/tarcisiolzbraga/codeflix/mail/service"
)

const pedidoValido = `{"to":"assinante@codeflix.com.br","firstname":"Tarcisio",` +
	`"confirmationUrl":"https://codeflix.test/cadastro/confirmar?token=abc"}`

type filaFalsa struct {
	tentativas []int
	atrasos    []time.Duration
	enterradas []string
	erroRetry  error
}

func (f *filaFalsa) Retry(_ context.Context, _ []byte, tentativa int, atraso time.Duration) error {
	if f.erroRetry != nil {
		return f.erroRetry
	}
	f.tentativas = append(f.tentativas, tentativa)
	f.atrasos = append(f.atrasos, atraso)
	return nil
}

func (f *filaFalsa) Bury(_ context.Context, _ []byte, motivo string) error {
	f.enterradas = append(f.enterradas, motivo)
	return nil
}

type enviadorFalso struct {
	enviadas []mail.Message
	erro     error
}

func (e *enviadorFalso) Send(msg mail.Message) error {
	if e.erro != nil {
		return e.erro
	}
	e.enviadas = append(e.enviadas, msg)
	return nil
}

func novo(fila service.Queue, enviador mail.Sender) *service.Service {
	log := slog.New(slog.NewTextHandler(io.Discard, nil))
	quem := mail.Identity{FromName: "Codeflix", FromAddress: "nao-responda@codeflix.com.br"}
	return service.New(fila, enviador, quem, log)
}

func TestHandle(t *testing.T) {
	t.Run("pedido válido vira e-mail enviado", func(t *testing.T) {
		fila, enviador := &filaFalsa{}, &enviadorFalso{}

		resultado := novo(fila, enviador).Handle(context.Background(), []byte(pedidoValido), 0)

		if resultado != service.Done {
			t.Errorf("esperava Done, veio %v", resultado)
		}
		if len(enviador.enviadas) != 1 {
			t.Fatalf("esperava um e-mail, veio %d", len(enviador.enviadas))
		}
		if enviador.enviadas[0].To != "assinante@codeflix.com.br" {
			t.Errorf("destinatário errado: %q", enviador.enviadas[0].To)
		}
	})

	// Mensagem que não dá para entender não volta para a fila: repetir não a conserta, e devolvê-la
	// a faria voltar para sempre.
	t.Run("pedido inválido é enterrado, não tentado de novo", func(t *testing.T) {
		fila, enviador := &filaFalsa{}, &enviadorFalso{}

		novo(fila, enviador).Handle(context.Background(), []byte(`{"to":"nao-e-email"}`), 0)

		if len(fila.tentativas) != 0 {
			t.Error("não deveria agendar nova tentativa para mensagem inválida")
		}
		if len(fila.enterradas) != 1 {
			t.Fatalf("esperava uma enterrada, veio %d", len(fila.enterradas))
		}
		if !strings.Contains(fila.enterradas[0], "payload inválido") {
			t.Errorf("o motivo deveria dizer que o payload é inválido: %q", fila.enterradas[0])
		}
		if len(enviador.enviadas) != 0 {
			t.Error("não deveria ter tentado enviar")
		}
	})

	// Falha de entrega é passageira: servidor de e-mail fora do ar volta.
	t.Run("falha ao enviar agenda nova tentativa com atraso que dobra", func(t *testing.T) {
		fila := &filaFalsa{}
		enviador := &enviadorFalso{erro: errors.New("conexão recusada")}
		s := novo(fila, enviador)

		s.Handle(context.Background(), []byte(pedidoValido), 0)
		s.Handle(context.Background(), []byte(pedidoValido), 1)
		s.Handle(context.Background(), []byte(pedidoValido), 2)

		if len(fila.tentativas) != 3 {
			t.Fatalf("esperava três agendamentos, veio %d", len(fila.tentativas))
		}
		esperados := []time.Duration{time.Second, 2 * time.Second, 4 * time.Second}
		for i, esperado := range esperados {
			if fila.atrasos[i] != esperado {
				t.Errorf("atraso %d: esperava %v, veio %v", i, esperado, fila.atrasos[i])
			}
		}
		if len(fila.enterradas) != 0 {
			t.Error("não deveria enterrar enquanto há tentativa pela frente")
		}
	})

	// Esgotada a escada, a mensagem vai para a fila morta — e não desaparece. Sem isso, um cadastro
	// que nunca recebeu e-mail não deixaria rastro.
	t.Run("esgotadas as tentativas, enterra", func(t *testing.T) {
		fila := &filaFalsa{}
		enviador := &enviadorFalso{erro: errors.New("conexão recusada")}

		novo(fila, enviador).Handle(context.Background(), []byte(pedidoValido), service.MaxAttempts-1)

		if len(fila.tentativas) != 0 {
			t.Error("não deveria agendar mais nada")
		}
		if len(fila.enterradas) != 1 {
			t.Fatalf("esperava uma enterrada, veio %d", len(fila.enterradas))
		}
		if !strings.Contains(fila.enterradas[0], "falha ao enviar") {
			t.Errorf("o motivo deveria dizer que a entrega falhou: %q", fila.enterradas[0])
		}
	})

	// Se nem agendar dá, enterrar é o que sobra: perder a mensagem em silêncio seria pior.
	t.Run("falha ao agendar também enterra", func(t *testing.T) {
		fila := &filaFalsa{erroRetry: errors.New("broker fora do ar")}
		enviador := &enviadorFalso{erro: errors.New("conexão recusada")}

		novo(fila, enviador).Handle(context.Background(), []byte(pedidoValido), 0)

		if len(fila.enterradas) != 1 {
			t.Fatalf("esperava uma enterrada, veio %d", len(fila.enterradas))
		}
		if !strings.Contains(fila.enterradas[0], "agendar") {
			t.Errorf("o motivo deveria dizer que o agendamento falhou: %q", fila.enterradas[0])
		}
	})

	// O endereço de quem se cadastrou não vai para o log: ele é lido por muita gente e indexado.
	t.Run("o endereço não aparece no log", func(t *testing.T) {
		var saida strings.Builder
		log := slog.New(slog.NewTextHandler(&saida, nil))
		quem := mail.Identity{FromName: "Codeflix", FromAddress: "nao-responda@codeflix.com.br"}
		s := service.New(&filaFalsa{}, &enviadorFalso{}, quem, log)

		s.Handle(context.Background(), []byte(pedidoValido), 0)

		if strings.Contains(saida.String(), "assinante@codeflix.com.br") {
			t.Errorf("o endereço não deveria estar no log: %s", saida.String())
		}
	})
}
