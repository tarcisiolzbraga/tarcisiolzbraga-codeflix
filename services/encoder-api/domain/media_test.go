package domain_test

import (
	"errors"
	"testing"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
)

var instante = time.Date(2026, 10, 9, 10, 0, 0, 0, time.UTC)

func Test_givenAllFields_whenNewMedia_thenGenerateTheIdAndKeepTheRest(t *testing.T) {
	videoID, checksum, filePath := "11111111-1111-1111-1111-111111111111", "abc123", "11111111/VIDEO"

	actual, err := domain.NewMedia(videoID, domain.MediaTypeVideo, checksum, filePath, instante)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if actual.ID == "" {
		t.Error("o id devia ser gerado aqui")
	}
	if actual.VideoID != videoID || actual.Checksum != checksum || actual.FilePath != filePath {
		t.Errorf("os campos recebidos deviam vir intactos, veio %+v", actual)
	}
	if actual.Type != domain.MediaTypeVideo {
		t.Errorf("esperava VIDEO, veio %q", actual.Type)
	}
	if !actual.CreatedAt.Equal(instante) {
		t.Errorf("esperava %v, veio %v", instante, actual.CreatedAt)
	}
}

func Test_givenTwoMedias_whenNewMedia_thenTheIdsDiffer(t *testing.T) {
	primeira, _ := domain.NewMedia("v1-id", domain.MediaTypeVideo, "abc", "caminho", instante)

	segunda, _ := domain.NewMedia("v1-id", domain.MediaTypeVideo, "abc", "caminho", instante)

	if primeira.ID == segunda.ID {
		t.Error("cada mídia precisa da própria pasta de saída, então os ids não podem repetir")
	}
}

// O checksum é o que o admin usa para descartar resposta atrasada. Sem ele a resposta chegaria sem
// chave, e o admin não teria como saber se é do envio atual ou do que foi substituído.
func Test_givenAMissingRequiredField_whenNewMedia_thenFailSayingWhich(t *testing.T) {
	casos := []struct {
		nome                        string
		videoID, checksum, filePath string
		esperado                    error
	}{
		{"sem videoId", "", "abc", "caminho", domain.ErrEmptyVideoID},
		{"sem checksum", "v1-id", "", "caminho", domain.ErrEmptyChecksum},
		{"sem filePath", "v1-id", "abc", "", domain.ErrEmptyFilePath},
	}
	for _, caso := range casos {
		t.Run(caso.nome, func(t *testing.T) {
			_, err := domain.NewMedia(caso.videoID, domain.MediaTypeVideo, caso.checksum, caso.filePath, instante)

			if !errors.Is(err, caso.esperado) {
				t.Errorf("esperava %v, veio %v", caso.esperado, err)
			}
		})
	}
}

func Test_givenAnUnknownType_whenNewMedia_thenFail(t *testing.T) {
	_, err := domain.NewMedia("v1-id", domain.MediaType("IMAGE"), "abc", "caminho", instante)

	if !errors.Is(err, domain.ErrUnknownMediaType) {
		t.Errorf("esperava tipo desconhecido, veio %v", err)
	}
}
