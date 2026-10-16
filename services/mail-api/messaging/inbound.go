// Package messaging é o contrato com quem pede e-mail.
package messaging

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/mail"
	"strings"
)

var ErrPayload = errors.New("mensagem inválida")

// SignUpValidation é o que a subscription-api publica em mail.signup.validation.queue.
//
// Os nomes dos campos são contrato com ela, e estão no record SignUpValidationMessage de lá: errar
// um deles não dá erro, só um e-mail sem destinatário ou sem link.
//
// Enxuto de propósito: para quem mandar, como chamar a pessoa, e o link pronto. Nada que obrigue
// este serviço a conhecer rota, token ou tabela do outro — o link vem montado, e é só isso que
// este serviço sabe sobre cadastro.
type SignUpValidation struct {
	To              string `json:"to"`
	Firstname       string `json:"firstname"`
	ConfirmationURL string `json:"confirmationUrl"`
}

// ParseSignUpValidation lê e valida a mensagem.
//
// Valida aqui, e não na hora de enviar: mensagem quebrada é descartada sem gastar conexão SMTP, e o
// motivo aparece no log junto do corpo que chegou. Repetir uma mensagem sem destinatário não a
// conserta, então ela não volta para a fila — vai direto para a fila morta.
func ParseSignUpValidation(body []byte) (SignUpValidation, error) {
	var msg SignUpValidation
	if err := json.Unmarshal(body, &msg); err != nil {
		return SignUpValidation{}, fmt.Errorf("%w: json: %w", ErrPayload, err)
	}
	if _, err := mail.ParseAddress(msg.To); err != nil {
		return SignUpValidation{}, fmt.Errorf("%w: 'to' não é um endereço: %q", ErrPayload, msg.To)
	}
	if strings.TrimSpace(msg.ConfirmationURL) == "" {
		return SignUpValidation{}, fmt.Errorf("%w: 'confirmationUrl' está vazio", ErrPayload)
	}
	// O primeiro nome pode faltar sem estragar nada: o e-mail passa a cumprimentar sem nome, o que
	// é melhor que descartar um cadastro de verdade por causa de uma saudação.
	msg.Firstname = strings.TrimSpace(msg.Firstname)
	return msg, nil
}
