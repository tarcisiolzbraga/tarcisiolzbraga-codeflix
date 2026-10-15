// Package queue é a conversa com o RabbitMQ: consome os pedidos de e-mail.
//
// Com rabbitmq/amqp091-go, como o encoder-api: o streadway/amqp do curso foi descontinuado e este é
// o sucessor oficial, mantido pela própria RabbitMQ a partir do mesmo código.
package queue

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

var ErrQueue = errors.New("falha na fila")

// Os nomes são contrato com a subscription-api, e estão no application.yml dela e no
// provisioning/rabbitmq/definitions.json. Errar um deles não dá erro, só silêncio — este serviço
// ficaria escutando uma fila que ninguém alimenta.
const (
	Exchange        = "mail.events"
	ValidationQueue = "mail.signup.validation.queue"

	// A escada de tentativas é **nossa**, não do contrato: estas filas não têm consumidor. A
	// mensagem fica na de espera o tempo do TTL e, ao expirar, o próprio broker a devolve para a
	// principal. É isso que dá nova tentativa sem ninguém dormindo segurando um operário.
	RetryQueue = "mail.signup.validation.retry.queue"
	DeadQueue  = "mail.signup.validation.dead.queue"

	// O cabeçalho que conta as tentativas. Sem ele não há como desistir, e a mensagem circularia
	// para sempre entre a fila de espera e a principal.
	AttemptHeader = "x-mail-attempt"
)

type Config struct {
	URL string
	// Prefetch é quantas mensagens o broker entrega antes de esperar confirmação. Mandar e-mail é
	// barato e rápido, bem diferente de converter vídeo, então aqui vale pegar um punhado.
	Prefetch int
}

type Client struct {
	conn    *amqp.Connection
	channel *amqp.Channel
}

func Connect(cfg Config) (*Client, error) {
	conn, err := amqp.Dial(cfg.URL)
	if err != nil {
		return nil, fmt.Errorf("%w: conectar: %w", ErrQueue, err)
	}
	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("%w: abrir canal: %w", ErrQueue, err)
	}
	prefetch := cfg.Prefetch
	if prefetch <= 0 {
		prefetch = 10
	}
	if err := channel.Qos(prefetch, 0, false); err != nil {
		conn.Close()
		return nil, fmt.Errorf("%w: prefetch: %w", ErrQueue, err)
	}
	return &Client{conn: conn, channel: channel}, nil
}

func (c *Client) Close() {
	if c.channel != nil {
		c.channel.Close()
	}
	if c.conn != nil {
		c.conn.Close()
	}
}

// DeclareTopology declara o que este serviço precisa, e só isso.
//
// A exchange e a fila principal já são declaradas pela subscription-api na subida dela e pelo
// definitions.json do broker. Declará-las aqui também é de propósito: declaração no AMQP é
// idempotente quando os argumentos casam, e isso deixa este serviço subir sozinho, sem depender da
// ordem de subida. **Divergir nos argumentos faz a subida falhar com PRECONDITION_FAILED**, o que é
// melhor que duas definições diferentes conviverem em silêncio.
//
// E isso não é teoria: a primeira versão disto declarava a exchange como `topic`, e o serviço não
// subiu — a de verdade é `direct`, como está no provisioning/rabbitmq/definitions.json. O tipo e os
// nomes vêm de lá; mudar um lado sem o outro faz a subida falhar, que é o que se quer.
func (c *Client) DeclareTopology() error {
	if err := c.channel.ExchangeDeclare(Exchange, "direct", true, false, false, false, nil); err != nil {
		return fmt.Errorf("%w: exchange: %w", ErrQueue, err)
	}
	if _, err := c.channel.QueueDeclare(ValidationQueue, true, false, false, false, nil); err != nil {
		return fmt.Errorf("%w: fila principal: %w", ErrQueue, err)
	}
	if err := c.channel.QueueBind(ValidationQueue, "mail.signup.validation", Exchange, false, nil); err != nil {
		return fmt.Errorf("%w: binding: %w", ErrQueue, err)
	}

	// Sem binding e sem consumidor: a mensagem é publicada direto nela, e ao expirar o
	// x-dead-letter a devolve para a principal.
	if _, err := c.channel.QueueDeclare(RetryQueue, true, false, false, false, amqp.Table{
		"x-dead-letter-exchange":    Exchange,
		"x-dead-letter-routing-key": "mail.signup.validation",
	}); err != nil {
		return fmt.Errorf("%w: fila de espera: %w", ErrQueue, err)
	}

	// A fila morta existe para alguém olhar, não para ser consumida. Sem ela, mensagem que esgotou
	// as tentativas desapareceria, e um cadastro que nunca recebeu e-mail não deixaria rastro.
	if _, err := c.channel.QueueDeclare(DeadQueue, true, false, false, false, nil); err != nil {
		return fmt.Errorf("%w: fila morta: %w", ErrQueue, err)
	}
	return nil
}

// Retry põe a mensagem na fila de espera, de onde o broker a devolve sozinho depois do atraso.
//
// O TTL vai por mensagem, e não na fila: assim uma fila só atende todos os degraus da escada, em
// vez de uma fila por atraso.
func (c *Client) Retry(ctx context.Context, payload []byte, tentativa int, atraso time.Duration) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	err := c.channel.PublishWithContext(ctx, "", RetryQueue, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Expiration:   strconv.FormatInt(atraso.Milliseconds(), 10),
		Headers:      amqp.Table{AttemptHeader: int32(tentativa)},
		Body:         payload,
	})
	if err != nil {
		return fmt.Errorf("%w: agendar nova tentativa: %w", ErrQueue, err)
	}
	return nil
}

// Bury manda a mensagem para a fila morta, com o motivo da desistência.
//
// O motivo vai num cabeçalho, e não no corpo: o corpo é o que a subscription-api publicou, e mexer
// nele faria quem olhar a fila morta ver uma mensagem que nunca existiu.
func (c *Client) Bury(ctx context.Context, payload []byte, motivo string) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	err := c.channel.PublishWithContext(ctx, "", DeadQueue, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Headers:      amqp.Table{"x-mail-dead-reason": motivo},
		Body:         payload,
	})
	if err != nil {
		return fmt.Errorf("%w: enterrar mensagem: %w", ErrQueue, err)
	}
	return nil
}

// Delivery é uma mensagem recebida, com o reconhecimento na mão de quem a processa.
type Delivery struct {
	Body []byte
	// Attempt é quantas vezes esta mensagem já foi tentada. Zero na primeira: o cabeçalho só existe
	// depois de a mensagem passar pela fila de espera.
	Attempt int
	raw     amqp.Delivery
}

// Ack confirma: a mensagem sai da fila.
func (d Delivery) Ack() error { return d.raw.Ack(false) }

// Discard descarta sem devolver à fila. É o que se faz com mensagem que não dá para entender:
// devolvê-la a faria voltar para sempre.
func (d Delivery) Discard() error { return d.raw.Nack(false, false) }

// Consume entrega as mensagens da fila de validação até o contexto ser cancelado.
func (c *Client) Consume(ctx context.Context, consumidor string) (<-chan Delivery, error) {
	entregas, err := c.channel.Consume(ValidationQueue, consumidor, false, false, false, false, nil)
	if err != nil {
		return nil, fmt.Errorf("%w: consumir: %w", ErrQueue, err)
	}
	saida := make(chan Delivery)
	go func() {
		defer close(saida)
		for {
			select {
			case <-ctx.Done():
				return
			case entrega, aberto := <-entregas:
				if !aberto {
					return
				}
				select {
				case saida <- Delivery{Body: entrega.Body, Attempt: attemptOf(entrega), raw: entrega}:
				case <-ctx.Done():
					return
				}
			}
		}
	}()
	return saida, nil
}

// attemptOf lê o contador do cabeçalho. O AMQP entrega inteiros em larguras diferentes conforme
// quem publicou, então vale tratar as que aparecem — int32 é o que nós publicamos, e int é o que
// alguns clientes mandam.
func attemptOf(entrega amqp.Delivery) int {
	switch valor := entrega.Headers[AttemptHeader].(type) {
	case int32:
		return int(valor)
	case int64:
		return int(valor)
	case int:
		return valor
	default:
		return 0
	}
}
