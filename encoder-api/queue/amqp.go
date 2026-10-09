// Package queue é a conversa com o RabbitMQ: consome o aviso do admin-codeflix e publica a
// resposta de volta.
//
// Com rabbitmq/amqp091-go, e não com o streadway/amqp do curso: aquele foi descontinuado e este é
// o sucessor oficial, mantido pela própria RabbitMQ a partir do mesmo código.
package queue

import (
	"context"
	"errors"
	"fmt"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

var ErrQueue = errors.New("falha na fila")

// A topologia é a mesma que o definitions.json provisiona e que o admin declara na subida. Os
// nomes são contrato: errar um deles não dá erro, só silêncio — a mensagem vai para um lugar que
// ninguém lê.
const (
	Exchange          = "video.events"
	CreatedQueue      = "video.created.queue"
	EncodedRoutingKey = "video.encoded"
)

type Config struct {
	URL string
	// Prefetch é quantas mensagens o broker entrega antes de esperar confirmação. Com 1, cada
	// consumidor pega a próxima só depois de terminar a atual: conversão é cara, e deixar várias
	// acumuladas num processo enquanto outro está ocioso só atrasa todo mundo.
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
		prefetch = 1
	}
	if err := channel.Qos(prefetch, 0, false); err != nil {
		conn.Close()
		return nil, fmt.Errorf("%w: prefetch: %w", ErrQueue, err)
	}
	return &Client{conn: conn, channel: channel}, nil
}

func (c *Client) Close() error {
	if err := c.channel.Close(); err != nil {
		c.conn.Close()
		return err
	}
	return c.conn.Close()
}

// DeclareTopology cria exchange, filas e bindings de forma idempotente.
//
// Existe pelo mesmo motivo que o AmqpConfig do admin: o definitions.json provisiona isso no broker
// do compose, mas um broker vazio — o de Testcontainers, por exemplo — precisa que alguém declare.
// Declarar algo diferente do que já existe dá PRECONDITION_FAILED, então os dois lados têm de
// concordar.
func (c *Client) DeclareTopology() error {
	if err := c.channel.ExchangeDeclare(Exchange, "direct", true, false, false, false, nil); err != nil {
		return fmt.Errorf("%w: exchange: %w", ErrQueue, err)
	}
	for fila, chave := range map[string]string{
		CreatedQueue:          "video.created",
		"video.encoded.queue": EncodedRoutingKey,
	} {
		if _, err := c.channel.QueueDeclare(fila, true, false, false, false, nil); err != nil {
			return fmt.Errorf("%w: fila %s: %w", ErrQueue, fila, err)
		}
		if err := c.channel.QueueBind(fila, chave, Exchange, false, nil); err != nil {
			return fmt.Errorf("%w: binding %s: %w", ErrQueue, fila, err)
		}
	}
	return nil
}

// Publish manda a resposta para o admin.
//
// DeliveryMode persistente e exchange durável: a resposta diz que o trabalho terminou, e perdê-la
// numa reinicialização do broker deixaria a mídia parada em PROCESSING para sempre.
func (c *Client) Publish(ctx context.Context, payload []byte) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	err := c.channel.PublishWithContext(ctx, Exchange, EncodedRoutingKey, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Body:         payload,
	})
	if err != nil {
		return fmt.Errorf("%w: publicar: %w", ErrQueue, err)
	}
	return nil
}

// Delivery é uma mensagem recebida, com o reconhecimento na mão de quem a processa.
type Delivery struct {
	Body []byte
	raw  amqp.Delivery
}

// Ack confirma: a mensagem sai da fila.
func (d Delivery) Ack() error { return d.raw.Ack(false) }

// Discard descarta sem devolver à fila. É o que se faz com mensagem que não dá para entender:
// devolvê-la a faria voltar para sempre, e é a mesma escolha que o listener do admin faz.
func (d Delivery) Discard() error { return d.raw.Nack(false, false) }

// Requeue devolve à fila, para outra tentativa. É para falha passageira — o Garage fora do ar, por
// exemplo —, em que tentar de novo tem chance de dar certo.
func (d Delivery) Requeue() error { return d.raw.Nack(false, true) }

// Consume entrega as mensagens de video.created.queue até o contexto ser cancelado.
func (c *Client) Consume(ctx context.Context, consumidor string) (<-chan Delivery, error) {
	entregas, err := c.channel.Consume(CreatedQueue, consumidor, false, false, false, false, nil)
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
				case saida <- Delivery{Body: entrega.Body, raw: entrega}:
				case <-ctx.Done():
					return
				}
			}
		}
	}()
	return saida, nil
}
