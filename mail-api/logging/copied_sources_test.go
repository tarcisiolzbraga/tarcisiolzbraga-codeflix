package logging_test

import (
	"go/ast"
	"go/parser"
	"go/printer"
	"go/token"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// Prova que o logging copiado entre os dois serviços Go **não divergiu**.
//
// O ecs.go do encoder-api e o deste serviço são o mesmo código de propósito: são poucas linhas,
// estáveis, e um módulo Go compartilhado entre dois serviços que não compartilham nada mais seria
// acoplamento por conveniência. A decisão está no comentário de cada cópia.
//
// O risco da cópia não é existir, é divergir em silêncio — e aqui não é hipótese: este logger
// engoliu a mensagem dos erros nos dois serviços (json.Marshal de um error devolve "{}"), e a
// correção precisou ser feita duas vezes porque nada ligava uma cópia à outra. Este teste troca
// esse risco por uma falha de build. O equivalente do lado Java é o CopiedSourcesTest da
// subscription-api.
//
// Compara o código, não o arquivo: cada cópia tem o seu próprio comentário dizendo de que serviço
// ela é. É o corpo que tem de ser igual, e é o que o go/parser devolve quando se pede o arquivo sem
// comentário nenhum.
const (
	encoderCopy = "encoder-api/logging/ecs.go"
	mailCopy    = "mail-api/logging/ecs.go"
)

func Test_givenTheEcsLoggingCopies_whenComparesTheirCode_thenTheyAreTheSame(t *testing.T) {
	// Given as duas cópias do logging, uma em cada serviço Go
	root := monorepoRoot(t)

	// When compara o código das duas, sem comentário
	doEncoder := codeOf(t, filepath.Join(root, encoderCopy))
	doMail := codeOf(t, filepath.Join(root, mailCopy))

	// Then são o mesmo código
	if doEncoder != doMail {
		t.Errorf("o código de %s divergiu de %s. As duas são cópia de propósito: ou a correção vale "+
			"para as duas, ou a decisão de copiar deixou de valer e elas precisam de um módulo comum.\n\n"+
			"--- %s\n%s\n\n--- %s\n%s",
			mailCopy, encoderCopy, encoderCopy, doEncoder, mailCopy, doMail)
	}
}

// Se o caminho mudar, o teste tem de falhar dizendo isso — e não passar por não ter achado nada para
// comparar, que é como um teste destes morre sem ninguém notar.
func Test_givenTheExpectedPaths_whenLooksForThem_thenEveryCopyIsThere(t *testing.T) {
	// Given os caminhos que o teste compara
	root := monorepoRoot(t)

	// When procura cada um
	for _, copia := range []string{encoderCopy, mailCopy} {
		// Then o arquivo está lá
		if info, err := os.Stat(filepath.Join(root, copia)); err != nil || !info.Mode().IsRegular() {
			t.Errorf("cópia não encontrada em %s: %v", copia, err)
		}
	}
}

// Sobe até achar a raiz, em vez de contar "..": o diretório de trabalho do teste é o do pacote, não
// o do serviço, e contar níveis quebraria se o teste mudasse de pacote. O build-logic é o marcador
// porque é ele que só existe na raiz.
func monorepoRoot(t *testing.T) string {
	t.Helper()
	candidato, err := os.Getwd()
	if err != nil {
		t.Fatalf("não consegui ler o diretório de trabalho: %v", err)
	}
	for {
		if info, err := os.Stat(filepath.Join(candidato, "build-logic")); err == nil && info.IsDir() {
			return candidato
		}
		pai := filepath.Dir(candidato)
		if pai == candidato {
			t.Fatalf("não achei a raiz do monorepo subindo a partir do diretório de trabalho")
		}
		candidato = pai
	}
}

// O arquivo reimpresso sem comentário nenhum: parser.ParseFile sem ParseComments descarta os
// comentários, e o printer devolve o resto em forma canônica, então espaço e quebra de linha não
// entram na comparação.
func codeOf(t *testing.T, caminho string) string {
	t.Helper()
	posicoes := token.NewFileSet()
	arquivo, err := parser.ParseFile(posicoes, caminho, nil, parser.SkipObjectResolution)
	if err != nil {
		t.Fatalf("não consegui ler %s: %v", caminho, err)
	}
	arquivo.Doc = nil
	// O mesmo FileSet do parse: com outro, o printer não acha as posições e devolve tudo numa linha.
	var impresso strings.Builder
	if err := (&printer.Config{Mode: printer.TabIndent, Tabwidth: 8}).Fprint(&impresso, posicoes, ast.Node(arquivo)); err != nil {
		t.Fatalf("não consegui reimprimir %s: %v", caminho, err)
	}
	return impresso.String()
}
