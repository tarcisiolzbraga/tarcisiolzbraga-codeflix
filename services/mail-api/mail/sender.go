package mail

import (
	"errors"
	"fmt"
	"net/smtp"
)

var ErrSend = errors.New("falha ao enviar e-mail")

// Sender entrega a mensagem. Interface para o consumidor poder ser testado sem SMTP de verdade.
type Sender interface {
	Send(Message) error
}

// SMTPConfig é o servidor de saída. Sem usuário e senha: em desenvolvimento o destino é um coletor
// local que não autentica, e este serviço não fala com provedor de verdade — é prova de conceito.
// Autenticação entra junto do provedor, com TLS, e não antes.
type SMTPConfig struct {
	Addr     string
	Identity Identity
}

type SMTPSender struct {
	cfg SMTPConfig
}

func NewSMTPSender(cfg SMTPConfig) *SMTPSender {
	return &SMTPSender{cfg: cfg}
}

// Send abre uma conexão por mensagem.
//
// É mais caro que manter uma aberta, e é a escolha certa aqui: conexão SMTP guardada morre em
// silêncio quando o servidor a derruba por inatividade, e o erro aparece na próxima mensagem, não
// na que causou. O volume deste serviço é um e-mail por cadastro.
func (s *SMTPSender) Send(msg Message) error {
	if err := smtp.SendMail(s.cfg.Addr, nil, s.cfg.Identity.FromAddress, []string{msg.To}, msg.Data); err != nil {
		return fmt.Errorf("%w: %w", ErrSend, err)
	}
	return nil
}
