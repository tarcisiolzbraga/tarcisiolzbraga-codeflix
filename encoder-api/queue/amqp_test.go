package queue_test

import (
	"context"
	"testing"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	"github.com/tarcisiolzbraga/codeflix/encoder/queue"
)

// RabbitMQ de verdade, na versão do compose. O que precisa ser provado é que a topologia que
// declaramos é a mesma que o admin declara: se divergir, o broker recusa com PRECONDITION_FAILED,
// e um dublê nunca diria isso.
func umRabbit(t *testing.T) queue.Config {
	t.Helper()
	if testing.Short() {
		t.Skip("precisa de Docker; pulado com -short")
	}
	ctx := context.Background()

	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: testcontainers.ContainerRequest{
			Image:        "rabbitmq:4.2-management",
			ExposedPorts: []string{"5672/tcp"},
			WaitingFor: wait.ForLog("Server startup complete").
				WithStartupTimeout(120 * time.Second),
		},
		Started: true,
	})
	if err != nil {
		t.Fatalf("não deu para subir o RabbitMQ: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })

	host, _ := container.Host(ctx)
	porta, err := container.MappedPort(ctx, "5672/tcp")
	if err != nil {
		t.Fatalf("não deu para descobrir a porta: %v", err)
	}
	return queue.Config{URL: "amqp://guest:guest@" + host + ":" + porta.Port() + "/", Prefetch: 1}
}

// Devolve também a configuração: as conexões de apoio do teste precisam do endereço, e o Client
// não o expõe — ele não tem por que expor.
func umCliente(t *testing.T) (*queue.Client, queue.Config) {
	t.Helper()
	cfg := umRabbit(t)
	cliente, err := queue.Connect(cfg)
	if err != nil {
		t.Fatalf("não deu para conectar: %v", err)
	}
	t.Cleanup(func() { _ = cliente.Close() })
	if err := cliente.DeclareTopology(); err != nil {
		t.Fatalf("a topologia devia ser declarada: %v", err)
	}
	return cliente, cfg
}

// O ciclo inteiro: o aviso entra por video.created, o encoder consome e responde, e a resposta
// aparece em video.encoded.queue, que é de onde o admin lê.
func Test_givenANoticeOnTheQueue_whenConsumeAndReply_thenTheAdminQueueHasTheAnswer(t *testing.T) {
	cliente, cfg := umCliente(t)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	publicarAviso(t, cfg, `{"videoId":"v1","type":"VIDEO"}`)

	entregas, err := cliente.Consume(ctx, "teste")
	if err != nil {
		t.Fatalf("não deu para consumir: %v", err)
	}
	var recebida queue.Delivery
	select {
	case recebida = <-entregas:
	case <-ctx.Done():
		t.Fatal("o aviso não chegou")
	}
	if err := cliente.Publish(ctx, []byte(`{"status":"COMPLETED","videoId":"v1"}`)); err != nil {
		t.Fatalf("não deu para publicar a resposta: %v", err)
	}
	_ = recebida.Ack()

	if string(recebida.Body) != `{"videoId":"v1","type":"VIDEO"}` {
		t.Errorf("o aviso veio diferente: %s", recebida.Body)
	}
	resposta := lerDaFilaDoAdmin(t, cfg)
	if resposta != `{"status":"COMPLETED","videoId":"v1"}` {
		t.Errorf("a resposta veio %q", resposta)
	}
}

// Declarar duas vezes tem de passar: o broker do compose já vem provisionado pelo definitions.json,
// e o encoder declara de novo na subida. Divergir daria PRECONDITION_FAILED.
func Test_givenAnAlreadyProvisionedBroker_whenDeclareAgain_thenAccept(t *testing.T) {
	cliente, _ := umCliente(t)

	err := cliente.DeclareTopology()

	if err != nil {
		t.Errorf("declarar de novo devia passar, veio %v", err)
	}
}

// Mensagem ilegível é descartada, não devolvida: devolver a faria voltar para sempre. É a mesma
// escolha que o listener do admin faz do outro lado.
func Test_givenADiscardedMessage_whenConsumeAgain_thenItDoesNotComeBack(t *testing.T) {
	cliente, cfg := umCliente(t)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	publicarAviso(t, cfg, `não é json`)
	entregas, _ := cliente.Consume(ctx, "teste-descarte")
	primeira := <-entregas

	_ = primeira.Discard()

	select {
	case repetida := <-entregas:
		t.Errorf("não devia voltar, veio %s", repetida.Body)
	case <-time.After(2 * time.Second):
	}
}

func publicarAviso(t *testing.T, cfg queue.Config, payload string) {
	t.Helper()
	conn, err := amqp.Dial(cfg.URL)
	if err != nil {
		t.Fatalf("não deu para abrir conexão de apoio: %v", err)
	}
	defer conn.Close()
	canal, err := conn.Channel()
	if err != nil {
		t.Fatalf("não deu para abrir canal de apoio: %v", err)
	}
	defer canal.Close()
	if err := canal.PublishWithContext(context.Background(), queue.Exchange, "video.created", false, false,
		amqp.Publishing{ContentType: "application/json", Body: []byte(payload)}); err != nil {
		t.Fatalf("não deu para publicar o aviso: %v", err)
	}
}

func lerDaFilaDoAdmin(t *testing.T, cfg queue.Config) string {
	t.Helper()
	conn, err := amqp.Dial(cfg.URL)
	if err != nil {
		t.Fatalf("não deu para abrir conexão de leitura: %v", err)
	}
	defer conn.Close()
	canal, _ := conn.Channel()
	defer canal.Close()
	for tentativa := 0; tentativa < 20; tentativa++ {
		msg, tem, err := canal.Get("video.encoded.queue", true)
		if err != nil {
			t.Fatalf("não deu para ler a fila do admin: %v", err)
		}
		if tem {
			return string(msg.Body)
		}
		time.Sleep(200 * time.Millisecond)
	}
	t.Fatal("a resposta não apareceu em video.encoded.queue")
	return ""
}

// O defeito que isto conserta: antes, falha passageira voltava à fila na hora e o laço girava
// quente. Agora a mensagem espera na fila de espera e o próprio broker a devolve.
func Test_givenATransientFailure_whenScheduleRetry_thenItComesBackAfterTheDelay(t *testing.T) {
	cliente, _ := umCliente(t)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	entregas, err := cliente.Consume(ctx, "teste-espera")
	if err != nil {
		t.Fatalf("não deu para consumir: %v", err)
	}

	if err := cliente.Retry(ctx, []byte(`{"videoId":"v1"}`), 1, 1500*time.Millisecond); err != nil {
		t.Fatalf("não deu para agendar: %v", err)
	}

	select {
	case <-entregas:
		t.Fatal("não devia voltar antes do atraso")
	case <-time.After(700 * time.Millisecond):
	}
	select {
	case devolvida := <-entregas:
		if string(devolvida.Body) != `{"videoId":"v1"}` {
			t.Errorf("voltou diferente: %s", devolvida.Body)
		}
		if devolvida.Attempt != 1 {
			t.Errorf("a contagem devia sobreviver à ida e volta, veio %d", devolvida.Attempt)
		}
		_ = devolvida.Ack()
	case <-ctx.Done():
		t.Fatal("a mensagem não voltou depois do atraso")
	}
}

// Sem o contador sobrevivendo, não há como desistir, e a mensagem circularia para sempre entre a
// fila de espera e a principal.
func Test_givenSeveralRetries_whenEachComesBack_thenTheCountGrows(t *testing.T) {
	cliente, _ := umCliente(t)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	entregas, _ := cliente.Consume(ctx, "teste-contagem")

	_ = cliente.Retry(ctx, []byte(`{"videoId":"v1"}`), 3, 300*time.Millisecond)

	select {
	case devolvida := <-entregas:
		if devolvida.Attempt != 3 {
			t.Errorf("esperava 3, veio %d", devolvida.Attempt)
		}
		_ = devolvida.Ack()
	case <-ctx.Done():
		t.Fatal("a mensagem não voltou")
	}
}
