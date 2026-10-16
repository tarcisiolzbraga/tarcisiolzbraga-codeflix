package mail_test

import (
	"strings"
	"testing"

	"github.com/tarcisiolzbraga/codeflix/mail/mail"
	"github.com/tarcisiolzbraga/codeflix/mail/messaging"
)

func quem() mail.Identity {
	return mail.Identity{FromName: "Codeflix", FromAddress: "nao-responda@codeflix.com.br"}
}

func Test_givenASignUpValidationNotice_whenBuildsTheEmail_thenItIsReadyToSend(t *testing.T) {
	msg := messaging.SignUpValidation{
		To:              "assinante@codeflix.com.br",
		Firstname:       "Tarcisio",
		ConfirmationURL: "https://codeflix.test/cadastro/confirmar?token=abc-123",
	}

	// O link é o único conteúdo que importa: um e-mail de validação sem ele não serve para nada.
	t.Run("o link vai no corpo", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if !strings.Contains(string(pronto.Data), msg.ConfirmationURL) {
			t.Error("o link deveria estar no corpo")
		}
	})

	t.Run("cumprimenta pelo primeiro nome", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if !strings.Contains(string(pronto.Data), "Olá, Tarcisio!") {
			t.Error("deveria cumprimentar pelo nome")
		}
	})

	t.Run("sem primeiro nome, cumprimenta sem nome", func(t *testing.T) {
		semNome := msg
		semNome.Firstname = ""

		pronto := mail.SignUpValidation(quem(), semNome)

		if !strings.Contains(string(pronto.Data), "Olá,") {
			t.Error("deveria cumprimentar de algum jeito")
		}
		if strings.Contains(string(pronto.Data), "Olá, !") {
			t.Error("não deveria deixar a saudação quebrada")
		}
	})

	// O assunto é ASCII e sai legível — o QEncoding só codifica quando precisa. A primeira versão
	// deste teste exigia que ele sempre saísse codificado, e falhou por afirmar o contrário do que
	// o código faz de certo.
	t.Run("o assunto sai legível", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if !strings.Contains(string(pronto.Data), "Subject: Confirme seu cadastro na Codeflix") {
			t.Errorf("o assunto deveria sair legível:\n%s", pronto.Data)
		}
	})

	// O corpo tem acento, e é o charset declarado que impede o cliente de mostrar bytes crus.
	t.Run("declara o charset do corpo", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if !strings.Contains(string(pronto.Data), "Content-Type: text/plain; charset=utf-8") {
			t.Error("deveria declarar text/plain em utf-8")
		}
	})

	// CRLF é o que o SMTP exige; LF sozinho faz servidor estrito recusar a mensagem.
	t.Run("separa cabeçalhos do corpo com uma linha em branco em CRLF", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if !strings.Contains(string(pronto.Data), "\r\n\r\n") {
			t.Error("faltou a linha em branco entre cabeçalhos e corpo")
		}
		if strings.Contains(strings.ReplaceAll(string(pronto.Data), "\r\n", ""), "\n") {
			t.Error("não deveria haver LF sozinho na mensagem")
		}
	})

	t.Run("o destinatário vai no envelope e no cabeçalho", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if pronto.To != msg.To {
			t.Errorf("envelope errado: %q", pronto.To)
		}
		if !strings.Contains(string(pronto.Data), "To: "+msg.To) {
			t.Error("o cabeçalho To deveria trazer o destinatário")
		}
	})

	// Diz o que acontece se a pessoa não pediu, que é a dúvida de quem recebe um e-mail pedindo
	// para clicar num link.
	t.Run("diz que ignorar é seguro", func(t *testing.T) {
		pronto := mail.SignUpValidation(quem(), msg)

		if !strings.Contains(string(pronto.Data), "ignore este e-mail") {
			t.Error("deveria dizer o que fazer se não foi a pessoa que pediu")
		}
	})
}
