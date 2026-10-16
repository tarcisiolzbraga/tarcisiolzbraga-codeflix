// Package mail monta e entrega o e-mail.
package mail

import (
	"fmt"
	"mime"
	"strings"

	"github.com/tarcisiolzbraga/codeflix/mail/messaging"
)

// From é quem assina o e-mail, e Subject o assunto. Vêm de configuração, não daqui.
type Identity struct {
	FromName    string
	FromAddress string
}

// Message é o e-mail pronto para entregar: o destinatário e os bytes da mensagem inteira.
type Message struct {
	To   string
	Data []byte
}

// SignUpValidation monta o e-mail de validação de cadastro.
//
// Texto puro, sem HTML. Não é economia: é o que não quebra em cliente nenhum, não pede imagem
// externa e não dá margem a o link clicável apontar para um lugar diferente do que está escrito —
// que é justamente a dúvida que alguém tem ao receber um e-mail pedindo para clicar.
//
// O assunto passa por mime.QEncoding, que **só codifica quando precisa**: o assunto de hoje é ASCII
// puro e sai legível, e no dia em que alguém puser um acento nele ele sai codificado em vez de
// mostrar bytes crus em cliente antigo. O corpo tem acento desde já, e quem resolve isso é o
// charset declarado no Content-Type.
func SignUpValidation(who Identity, msg messaging.SignUpValidation) Message {
	const subject = "Confirme seu cadastro na Codeflix"

	saudacao := "Olá,"
	if msg.Firstname != "" {
		saudacao = fmt.Sprintf("Olá, %s!", msg.Firstname)
	}

	corpo := strings.Join([]string{
		saudacao,
		"",
		"Para concluir seu cadastro na Codeflix, abra o endereço abaixo:",
		"",
		msg.ConfirmationURL,
		"",
		"O link vale por 24 horas. Se não foi você que pediu o cadastro, ignore este e-mail —",
		"nenhuma conta é criada sem essa confirmação.",
		"",
		"Codeflix",
	}, "\r\n")

	// Cabeçalhos e corpo separados por uma linha em branco, com CRLF: é o que o SMTP exige, e LF
	// sozinho faz servidor estrito recusar a mensagem.
	cabecalhos := []string{
		fmt.Sprintf("From: %s <%s>", who.FromName, who.FromAddress),
		fmt.Sprintf("To: %s", msg.To),
		fmt.Sprintf("Subject: %s", mime.QEncoding.Encode("utf-8", subject)),
		"MIME-Version: 1.0",
		"Content-Type: text/plain; charset=utf-8",
	}

	return Message{
		To:   msg.To,
		Data: []byte(strings.Join(cabecalhos, "\r\n") + "\r\n\r\n" + corpo + "\r\n"),
	}
}
