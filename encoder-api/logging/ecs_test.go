package logging_test

import (
	"bytes"
	"encoding/json"
	"log/slog"
	"sync"
	"testing"

	"github.com/tarcisiolzbraga/codeflix/encoder/logging"
)

func umaLinha(t *testing.T, escrever func(*slog.Logger)) map[string]any {
	t.Helper()
	var buf bytes.Buffer
	escrever(logging.New(&buf, slog.LevelInfo, "encoder-api"))
	var linha map[string]any
	if err := json.Unmarshal(buf.Bytes(), &linha); err != nil {
		t.Fatalf("a saída devia ser JSON: %v (%s)", err, buf.String())
	}
	return linha
}

func aninhado(t *testing.T, linha map[string]any, grupo, campo string) any {
	t.Helper()
	g, ok := linha[grupo].(map[string]any)
	if !ok {
		t.Fatalf("%q devia ser objeto aninhado, veio %T", grupo, linha[grupo])
	}
	return g[campo]
}

// São exatamente os campos que a pilha consulta: o alerta da fila morta filtra por log.level e
// message, e o Kibana agrupa por service.name. Nomes diferentes fazem o log aparecer sem ser
// encontrável, que é pior do que não chegar.
func Test_givenAMessage_whenLog_thenUseTheFieldNamesTheStackQueries(t *testing.T) {
	linha := umaLinha(t, func(l *slog.Logger) { l.Error("mensagem descartada para a fila morta") })

	if _, tem := linha["@timestamp"]; !tem {
		t.Errorf("faltou @timestamp; veio %v", chaves(linha))
	}
	if linha["message"] != "mensagem descartada para a fila morta" {
		t.Errorf("message veio %v", linha["message"])
	}
	if nivel := aninhado(t, linha, "log", "level"); nivel != "ERROR" {
		t.Errorf("log.level veio %v", nivel)
	}
	if nome := aninhado(t, linha, "service", "name"); nome != "encoder-api" {
		t.Errorf("service.name veio %v", nome)
	}
	if v := aninhado(t, linha, "ecs", "version"); v != logging.Version {
		t.Errorf("ecs.version veio %v", v)
	}
}

// Os nomes que o slog usa por padrão não podem sobrar: se "msg" continuar lá ao lado de "message",
// o documento fica com o mesmo dado em dois campos e a consulta passa a depender de qual deles.
func Test_givenAMessage_whenLog_thenDropTheDefaultSlogNames(t *testing.T) {
	linha := umaLinha(t, func(l *slog.Logger) { l.Info("qualquer") })

	for _, antigo := range []string{"time", "level", "msg"} {
		if _, tem := linha[antigo]; tem {
			t.Errorf("%q não devia sobrar; campos: %v", antigo, chaves(linha))
		}
	}
}

// Atributo do chamador continua como veio: o renomeio vale só para os três campos que o slog emite
// sozinho. Renomear "level" dentro de um grupo de quem chama seria mexer em dado alheio.
func Test_givenCallerAttributes_whenLog_thenKeepThemUntouched(t *testing.T) {
	linha := umaLinha(t, func(l *slog.Logger) {
		l.Info("convertendo", "videoId", "v1-id", "tentativa", 2)
	})

	if linha["videoId"] != "v1-id" {
		t.Errorf("videoId veio %v", linha["videoId"])
	}
	if linha["tentativa"] != float64(2) {
		t.Errorf("tentativa veio %v", linha["tentativa"])
	}
}

func Test_givenADebugMessage_whenLevelIsInfo_thenWriteNothing(t *testing.T) {
	var buf bytes.Buffer
	logging.New(&buf, slog.LevelInfo, "encoder-api").Debug("não deve sair")

	if buf.Len() != 0 {
		t.Errorf("não devia escrever nada, veio %q", buf.String())
	}
}

func chaves(m map[string]any) []string {
	var k []string
	for chave := range m {
		k = append(k, chave)
	}
	return k
}

// O encoder converte em paralelo, então vários operários escrevem no mesmo descritor. Sem trava,
// duas linhas se intercalam e ambas deixam de ser JSON — e o coletor descarta as duas.
func Test_givenConcurrentWriters_whenLog_thenEveryLineIsWholeJSON(t *testing.T) {
	var buf seguro
	registrador := logging.New(&buf, slog.LevelInfo, "encoder-api")
	var grupo sync.WaitGroup

	for i := 0; i < 50; i++ {
		grupo.Add(1)
		go func(n int) {
			defer grupo.Done()
			registrador.Info("convertendo", "operario", n)
		}(i)
	}
	grupo.Wait()

	linhas := bytes.Split(bytes.TrimSpace(buf.conteudo()), []byte("\n"))
	if len(linhas) != 50 {
		t.Fatalf("esperava 50 linhas, vieram %d", len(linhas))
	}
	for i, linha := range linhas {
		var m map[string]any
		if err := json.Unmarshal(linha, &m); err != nil {
			t.Fatalf("a linha %d não é JSON inteiro: %v (%s)", i, err, linha)
		}
	}
}

// Buffer com trava própria: sem ela o -race acusaria o próprio teste, e não o handler.
type seguro struct {
	mu  sync.Mutex
	buf bytes.Buffer
}

func (s *seguro) Write(p []byte) (int, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.buf.Write(p)
}

func (s *seguro) conteudo() []byte {
	s.mu.Lock()
	defer s.mu.Unlock()
	return append([]byte{}, s.buf.Bytes()...)
}
