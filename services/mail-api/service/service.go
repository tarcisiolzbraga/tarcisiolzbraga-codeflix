// Package service decide o que fazer com cada pedido de e-mail.
package service

import (
	"context"
	"log/slog"
	"time"

	"github.com/tarcisiolzbraga/codeflix/mail/mail"
	"github.com/tarcisiolzbraga/codeflix/mail/messaging"
)

// Queue é o que o serviço precisa da fila. Interface aqui, e não o cliente concreto, para o serviço
// ser testado sem broker.
type Queue interface {
	Retry(ctx context.Context, payload []byte, tentativa int, atraso time.Duration) error
	Bury(ctx context.Context, payload []byte, motivo string) error
}

// MaxAttempts é quantas vezes uma mensagem é tentada antes de ir para a fila morta.
//
// Cinco com o atraso dobrando — 1s, 2s, 4s, 8s, 16s — tolera meio minuto de servidor de e-mail
// fora do ar. Mais que isso não ajuda: o link vale 24 horas, mas quem se cadastrou está olhando a
// tela agora, e um e-mail que chega em dez minutos já perdeu a pessoa.
const MaxAttempts = 5

type Service struct {
	queue  Queue
	sender mail.Sender
	who    mail.Identity
	log    *slog.Logger
}

func New(queue Queue, sender mail.Sender, who mail.Identity, log *slog.Logger) *Service {
	return &Service{queue: queue, sender: sender, who: who, log: log}
}

// Outcome é o que o chamador deve fazer com a mensagem depois.
type Outcome int

const (
	// Done: o e-mail saiu, ou a mensagem não tinha conserto. Nos dois casos ela sai da fila.
	Done Outcome = iota
	// Discard: a mensagem não dá para entender e já foi enterrada.
	Discard
)

// Handle processa um pedido de e-mail.
//
// Mensagem que não dá para entender **não volta para a fila**: repetir não a conserta, e devolvê-la
// a faria voltar para sempre. Ela vai para a fila morta com o motivo, que é o que permite alguém
// descobrir que um cadastro nunca recebeu e-mail.
//
// Falha de entrega é outra coisa: servidor de e-mail fora do ar é passageiro, então a mensagem
// volta pela escada de tentativas. Esgotada a escada, também vai para a fila morta.
func (s *Service) Handle(ctx context.Context, body []byte, tentativa int) Outcome {
	pedido, err := messaging.ParseSignUpValidation(body)
	if err != nil {
		s.log.Error("pedido de e-mail inválido; enterrando", "erro", err, "corpo", string(body))
		s.bury(ctx, body, "payload inválido: "+err.Error())
		return Discard
	}

	// O endereço não vai para o log, e é de propósito: o log é lido por muita gente e indexado, e
	// o e-mail de quem se cadastrou não precisa estar lá. O que identifica a mensagem é a
	// tentativa; quem precisar do endereço acha na fila morta.
	if err := s.sender.Send(mail.SignUpValidation(s.who, pedido)); err != nil {
		s.retryOrBury(ctx, body, tentativa, err)
		return Discard
	}

	s.log.Info("e-mail de validação enviado", "tentativa", tentativa+1)
	return Done
}

func (s *Service) retryOrBury(ctx context.Context, body []byte, tentativa int, causa error) {
	proxima := tentativa + 1
	if proxima >= MaxAttempts {
		s.log.Error("desisti de enviar o e-mail", "tentativas", proxima, "erro", causa)
		s.bury(ctx, body, "falha ao enviar: "+causa.Error())
		return
	}
	atraso := backoff(proxima)
	s.log.Warn("falha ao enviar; nova tentativa agendada",
		"tentativa", proxima, "atraso", atraso.String(), "erro", causa)
	if err := s.queue.Retry(ctx, body, proxima, atraso); err != nil {
		// Não deu para agendar: enterrar é o que sobra. Perder a mensagem em silêncio seria pior.
		s.log.Error("não deu para agendar nova tentativa; enterrando", "erro", err)
		s.bury(ctx, body, "falha ao agendar nova tentativa: "+err.Error())
	}
}

func (s *Service) bury(ctx context.Context, body []byte, motivo string) {
	if err := s.queue.Bury(ctx, body, motivo); err != nil {
		s.log.Error("não deu para enterrar a mensagem", "erro", err, "motivo", motivo)
	}
}

// backoff dobra o atraso a cada tentativa: 1s, 2s, 4s, 8s.
func backoff(tentativa int) time.Duration {
	atraso := time.Second
	for i := 1; i < tentativa; i++ {
		atraso *= 2
	}
	return atraso
}
