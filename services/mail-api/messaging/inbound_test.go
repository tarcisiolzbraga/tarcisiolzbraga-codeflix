package messaging_test

import (
	"errors"
	"testing"

	"github.com/tarcisiolzbraga/codeflix/mail/messaging"
)

func TestParseSignUpValidation(t *testing.T) {
	t.Run("lê a mensagem que a subscription-api publica", func(t *testing.T) {
		corpo := []byte(`{"to":"assinante@codeflix.com.br","firstname":"Tarcisio",` +
			`"confirmationUrl":"https://codeflix.test/cadastro/confirmar?token=abc"}`)

		msg, err := messaging.ParseSignUpValidation(corpo)

		if err != nil {
			t.Fatalf("não esperava erro, veio: %v", err)
		}
		if msg.To != "assinante@codeflix.com.br" {
			t.Errorf("destinatário errado: %q", msg.To)
		}
		if msg.ConfirmationURL == "" {
			t.Error("o link deveria ter chegado")
		}
	})

	// Os nomes dos campos são contrato com o outro serviço. Se alguém os renomear de um lado, é
	// aqui que aparece — e não num e-mail sem destinatário já em produção.
	t.Run("recusa quando o campo do destinatário tem outro nome", func(t *testing.T) {
		corpo := []byte(`{"email":"assinante@codeflix.com.br","confirmationUrl":"https://x/y"}`)

		_, err := messaging.ParseSignUpValidation(corpo)

		if !errors.Is(err, messaging.ErrPayload) {
			t.Fatalf("esperava ErrPayload, veio: %v", err)
		}
	})

	t.Run("recusa endereço que não é endereço", func(t *testing.T) {
		corpo := []byte(`{"to":"nao-e-email","confirmationUrl":"https://x/y"}`)

		_, err := messaging.ParseSignUpValidation(corpo)

		if !errors.Is(err, messaging.ErrPayload) {
			t.Fatalf("esperava ErrPayload, veio: %v", err)
		}
	})

	t.Run("recusa sem link, que é o único conteúdo que importa", func(t *testing.T) {
		corpo := []byte(`{"to":"assinante@codeflix.com.br","confirmationUrl":"   "}`)

		_, err := messaging.ParseSignUpValidation(corpo)

		if !errors.Is(err, messaging.ErrPayload) {
			t.Fatalf("esperava ErrPayload, veio: %v", err)
		}
	})

	// Primeiro nome é saudação, não requisito: descartar um cadastro de verdade por causa dele
	// seria pior que mandar um e-mail sem nome.
	t.Run("aceita sem primeiro nome", func(t *testing.T) {
		corpo := []byte(`{"to":"assinante@codeflix.com.br","confirmationUrl":"https://x/y"}`)

		msg, err := messaging.ParseSignUpValidation(corpo)

		if err != nil {
			t.Fatalf("não esperava erro, veio: %v", err)
		}
		if msg.Firstname != "" {
			t.Errorf("esperava nome vazio, veio %q", msg.Firstname)
		}
	})

	t.Run("recusa json quebrado", func(t *testing.T) {
		_, err := messaging.ParseSignUpValidation([]byte(`{nao e json`))

		if !errors.Is(err, messaging.ErrPayload) {
			t.Fatalf("esperava ErrPayload, veio: %v", err)
		}
	})
}
