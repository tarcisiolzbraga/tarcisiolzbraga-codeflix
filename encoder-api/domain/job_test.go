package domain_test

import (
	"errors"
	"testing"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
)

func umaMedia(t *testing.T) *domain.Media {
	t.Helper()
	media, err := domain.NewMedia("v1-id", domain.MediaTypeVideo, "abc123", "v1-id/VIDEO", instante)
	if err != nil {
		t.Fatalf("a mídia de apoio devia nascer válida, veio %v", err)
	}
	return media
}

// PROCESSING não é só estado interno: é a primeira das três respostas do contrato, e é ela que tira
// a mídia de PENDING no admin.
func Test_givenAMedia_whenNewJob_thenStartProcessing(t *testing.T) {
	media := umaMedia(t)

	actual, err := domain.NewJob("encoded/v1-id", media, instante)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if actual.Status != domain.StatusProcessing {
		t.Errorf("esperava PROCESSING, veio %q", actual.Status)
	}
	if actual.ID == "" {
		t.Error("o id devia ser gerado aqui")
	}
	if actual.Media != media {
		t.Error("o trabalho devia guardar a mídia recebida")
	}
}

func Test_givenNoMedia_whenNewJob_thenFail(t *testing.T) {
	_, err := domain.NewJob("encoded/v1-id", nil, instante)

	if !errors.Is(err, domain.ErrNoMedia) {
		t.Errorf("esperava falta de mídia, veio %v", err)
	}
}

func Test_givenNoOutputPath_whenNewJob_thenFail(t *testing.T) {
	_, err := domain.NewJob("", umaMedia(t), instante)

	if !errors.Is(err, domain.ErrEmptyOutputPath) {
		t.Errorf("esperava caminho de saída vazio, veio %v", err)
	}
}

func Test_givenAProcessingJob_whenComplete_thenMoveItAndTouchUpdatedAt(t *testing.T) {
	job, _ := domain.NewJob("encoded/v1-id", umaMedia(t), instante)
	depois := instante.Add(90 * 1e9)

	err := job.Complete(depois)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if job.Status != domain.StatusCompleted {
		t.Errorf("esperava COMPLETED, veio %q", job.Status)
	}
	if !job.UpdatedAt.Equal(depois) {
		t.Errorf("esperava %v, veio %v", depois, job.UpdatedAt)
	}
}

// O motivo viaja junto do status porque o admin só registra o ERROR em log — sem ele seria log
// vazio, e ninguém saberia por que a mídia não converteu.
func Test_givenAProcessingJob_whenFail_thenKeepTheReason(t *testing.T) {
	job, _ := domain.NewJob("encoded/v1-id", umaMedia(t), instante)

	err := job.Fail("codec não suportado", instante)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	if job.Status != domain.StatusError {
		t.Errorf("esperava ERROR, veio %q", job.Status)
	}
	if job.Error != "codec não suportado" {
		t.Errorf("esperava o motivo guardado, veio %q", job.Error)
	}
}

// Concluir o que já falhou, ou falhar o que já concluiu, é erro de controle de fluxo. Deixar passar
// mandaria ao admin duas respostas finais contraditórias para a mesma mídia.
func Test_givenAFinishedJob_whenFinishAgain_thenRefuse(t *testing.T) {
	concluido, _ := domain.NewJob("encoded/v1-id", umaMedia(t), instante)
	_ = concluido.Complete(instante)
	comErro, _ := domain.NewJob("encoded/v1-id", umaMedia(t), instante)
	_ = comErro.Fail("qualquer motivo", instante)

	erroAoConcluir := comErro.Complete(instante)
	erroAoFalhar := concluido.Fail("tarde demais", instante)

	if !errors.Is(erroAoConcluir, domain.ErrInvalidStatus) {
		t.Errorf("concluir o que falhou devia ser recusado, veio %v", erroAoConcluir)
	}
	if !errors.Is(erroAoFalhar, domain.ErrInvalidStatus) {
		t.Errorf("falhar o que concluiu devia ser recusado, veio %v", erroAoFalhar)
	}
}
