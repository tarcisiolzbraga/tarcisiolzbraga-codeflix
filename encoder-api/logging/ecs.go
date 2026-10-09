// Package logging põe o log do encoder no mesmo formato dos outros dois serviços.
//
// Não é preciosismo: a pilha de observabilidade consulta por nomes de campo. O Logstash extrai
// pares de "message", o alerta da fila morta filtra por "log.level", e o Kibana agrupa por
// "service.name". Com os nomes que o slog usa por padrão — time, level, msg — o log do encoder
// chega ao Elasticsearch mas não se mistura com o resto: aparece, e não é encontrável.
package logging

import (
	"context"
	"encoding/json"
	"io"
	"log/slog"
	"sync"
	"time"
)

// Version é a versão do ECS que os outros serviços declaram. Fica junto para quem consulta saber
// que os três falam o mesmo dialeto.
const Version = "8.11"

// New devolve um logger que escreve JSON em ECS.
func New(w io.Writer, level slog.Level, service string) *slog.Logger {
	return slog.New(&ecsHandler{saida: w, nivel: level, servico: service, mutex: &sync.Mutex{}})
}

// Handler próprio, e não JSONHandler com ReplaceAttr: devolver um grupo de dentro do ReplaceAttr
// faz o slog chamá-lo de novo para o conteúdo do grupo, e o renomeio entra em recursão. Montar o
// documento à mão é mais código e não tem esse tipo de surpresa.
type ecsHandler struct {
	saida   io.Writer
	nivel   slog.Level
	servico string
	fixos   []slog.Attr
	// O escritor é compartilhado entre os operários do encoder, e sem o mutex duas linhas podem se
	// intercalar no meio — o que quebra o JSON de ambas. Criado em New e compartilhado pelas cópias
	// que WithAttrs faz: criá-lo sob demanda seria a própria corrida que ele existe para evitar.
	mutex *sync.Mutex
}

func (h *ecsHandler) Enabled(_ context.Context, nivel slog.Level) bool {
	return nivel >= h.nivel
}

func (h *ecsHandler) Handle(_ context.Context, registro slog.Record) error {
	documento := map[string]any{
		"@timestamp": registro.Time.UTC().Format(time.RFC3339Nano),
		"log":        map[string]any{"level": registro.Level.String()},
		"message":    registro.Message,
		"service":    map[string]any{"name": h.servico},
		"ecs":        map[string]any{"version": Version},
	}
	for _, a := range h.fixos {
		documento[a.Key] = a.Value.Any()
	}
	registro.Attrs(func(a slog.Attr) bool {
		documento[a.Key] = a.Value.Any()
		return true
	})

	linha, err := json.Marshal(documento)
	if err != nil {
		return err
	}
	h.mutex.Lock()
	defer h.mutex.Unlock()
	_, err = h.saida.Write(append(linha, '\n'))
	return err
}

func (h *ecsHandler) WithAttrs(atributos []slog.Attr) slog.Handler {
	novo := *h
	novo.fixos = append(append([]slog.Attr{}, h.fixos...), atributos...)
	return &novo
}

// WithGroup devolve o mesmo handler: o encoder não agrupa atributos, e aninhar aqui só
// acrescentaria caminho de código sem uso para alguém manter.
func (h *ecsHandler) WithGroup(string) slog.Handler { return h }
