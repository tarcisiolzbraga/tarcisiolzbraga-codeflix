package messaging_test

import (
	"errors"
	"testing"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
	"github.com/tarcisiolzbraga/codeflix/encoder/messaging"
)

var agora = time.Date(2026, 10, 9, 10, 0, 0, 0, time.UTC)

// O payload é o do record VideoMediaCreated do admin, com os nomes que o Jackson gera. Se algum
// campo for renomeado de um dos lados, é este teste que avisa.
const avisoDoAdmin = `{
  "videoId": "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608",
  "type": "VIDEO",
  "filePath": "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608/VIDEO",
  "checksum": "d41d8cd98f00b204e9800998ecf8427e",
  "occurredOn": "2026-09-29T12:00:00Z"
}`

func Test_givenTheAdminNotice_whenParse_thenBuildTheMedia(t *testing.T) {
	actual, err := messaging.ParseVideoMediaCreated([]byte(avisoDoAdmin), agora)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if actual.VideoID != "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608" {
		t.Errorf("videoId veio %q", actual.VideoID)
	}
	if actual.Type != domain.MediaTypeVideo {
		t.Errorf("type veio %q", actual.Type)
	}
	if actual.Checksum != "d41d8cd98f00b204e9800998ecf8427e" {
		t.Errorf("checksum veio %q", actual.Checksum)
	}
	if actual.FilePath != "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608/VIDEO" {
		t.Errorf("filePath veio %q", actual.FilePath)
	}
}

func Test_givenATrailer_whenParse_thenAcceptIt(t *testing.T) {
	payload := `{"videoId":"v1","type":"TRAILER","filePath":"v1/TRAILER","checksum":"abc","occurredOn":"2026-09-29T12:00:00Z"}`

	actual, err := messaging.ParseVideoMediaCreated([]byte(payload), agora)

	if err != nil {
		t.Fatalf("TRAILER devia ser aceito, veio %v", err)
	}
	if actual.Type != domain.MediaTypeTrailer {
		t.Errorf("esperava TRAILER, veio %q", actual.Type)
	}
}

// O admin não publica aviso para imagem. Se chegar, é mensagem que não devia existir: recusar é
// melhor que converter o que não é vídeo.
func Test_givenATypeTheAdminDoesNotPublish_whenParse_thenFail(t *testing.T) {
	payload := `{"videoId":"v1","type":"BANNER","filePath":"v1/BANNER","checksum":"abc","occurredOn":"2026-09-29T12:00:00Z"}`

	_, err := messaging.ParseVideoMediaCreated([]byte(payload), agora)

	if !errors.Is(err, domain.ErrUnknownMediaType) {
		t.Errorf("esperava tipo desconhecido, veio %v", err)
	}
}

func Test_givenGarbage_whenParse_thenFailWithoutPanicking(t *testing.T) {
	for _, payload := range []string{"", "{", "não é json", "[]"} {
		_, err := messaging.ParseVideoMediaCreated([]byte(payload), agora)

		if err == nil {
			t.Errorf("%q devia ser recusado", payload)
		}
	}
}

// Campo faltando desserializa sem erro em Go, virando string vazia — quem barra é a validação do
// domínio. Sem este teste, um aviso truncado viraria mídia sem checksum.
func Test_givenANoticeMissingTheChecksum_whenParse_thenFail(t *testing.T) {
	payload := `{"videoId":"v1","type":"VIDEO","filePath":"v1/VIDEO","occurredOn":"2026-09-29T12:00:00Z"}`

	_, err := messaging.ParseVideoMediaCreated([]byte(payload), agora)

	if !errors.Is(err, domain.ErrEmptyChecksum) {
		t.Errorf("esperava checksum vazio, veio %v", err)
	}
}
