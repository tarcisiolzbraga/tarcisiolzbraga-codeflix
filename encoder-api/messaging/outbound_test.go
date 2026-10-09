package messaging_test

import (
	"encoding/json"
	"testing"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
	"github.com/tarcisiolzbraga/codeflix/encoder/messaging"
)

func umTrabalho(t *testing.T) *domain.Job {
	t.Helper()
	media, err := domain.NewMedia("v1-id", domain.MediaTypeVideo, "abc123", "v1-id/VIDEO", agora)
	if err != nil {
		t.Fatalf("a mídia de apoio devia nascer válida: %v", err)
	}
	job, err := domain.NewJob("encoded/v1-id", media, agora)
	if err != nil {
		t.Fatalf("o trabalho de apoio devia nascer válido: %v", err)
	}
	return job
}

func campos(t *testing.T, payload []byte) map[string]any {
	t.Helper()
	var m map[string]any
	if err := json.Unmarshal(payload, &m); err != nil {
		t.Fatalf("a saída devia ser JSON: %v", err)
	}
	return m
}

// É o status que o Jackson do admin usa para escolher entre os três records. Errá-lo faz a
// mensagem ser descartada do outro lado sem explicação.
func Test_givenANewJob_whenEncode_thenSayProcessing(t *testing.T) {
	job := umTrabalho(t)

	payload, err := messaging.Encode(job)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	m := campos(t, payload)
	if m["status"] != "PROCESSING" {
		t.Errorf("status veio %v", m["status"])
	}
	if m["videoId"] != "v1-id" || m["type"] != "VIDEO" || m["checksum"] != "abc123" {
		t.Errorf("os três campos comuns vieram %v", m)
	}
	if _, tem := m["encodedPath"]; tem {
		t.Error("PROCESSING não leva encodedPath: o record do admin não tem esse campo")
	}
}

func Test_givenACompletedJob_whenEncode_thenCarryTheEncodedPath(t *testing.T) {
	job := umTrabalho(t)
	_ = job.Complete(agora)

	payload, err := messaging.Encode(job)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	m := campos(t, payload)
	if m["status"] != "COMPLETED" {
		t.Errorf("status veio %v", m["status"])
	}
	if m["encodedPath"] != "encoded/v1-id" {
		t.Errorf("encodedPath veio %v", m["encodedPath"])
	}
	if _, tem := m["message"]; tem {
		t.Error("COMPLETED não leva message")
	}
}

func Test_givenAFailedJob_whenEncode_thenCarryTheReason(t *testing.T) {
	job := umTrabalho(t)
	_ = job.Fail("codec não suportado", agora)

	payload, err := messaging.Encode(job)

	if err != nil {
		t.Fatalf("não esperava erro, veio %v", err)
	}
	m := campos(t, payload)
	if m["status"] != "ERROR" {
		t.Errorf("status veio %v", m["status"])
	}
	if m["message"] != "codec não suportado" {
		t.Errorf("message veio %v", m["message"])
	}
	if _, tem := m["encodedPath"]; tem {
		t.Error("ERROR não leva encodedPath")
	}
}

// O checksum é a razão de o admin conseguir descartar resposta atrasada. Ele precisa voltar igual
// ao que chegou, nas três formas.
func Test_givenAnyStatus_whenEncode_thenEchoTheChecksum(t *testing.T) {
	emProcesso := umTrabalho(t)
	concluido := umTrabalho(t)
	_ = concluido.Complete(agora)
	comErro := umTrabalho(t)
	_ = comErro.Fail("qualquer", agora)

	for _, job := range []*domain.Job{emProcesso, concluido, comErro} {
		payload, err := messaging.Encode(job)

		if err != nil {
			t.Fatalf("não esperava erro, veio %v", err)
		}
		if campos(t, payload)["checksum"] != "abc123" {
			t.Errorf("o checksum devia voltar igual em %q", job.Status)
		}
	}
}

func Test_givenAJobWithoutMedia_whenEncode_thenFail(t *testing.T) {
	_, err := messaging.Encode(&domain.Job{Status: domain.StatusProcessing})

	if err == nil {
		t.Error("trabalho sem mídia não tem o que responder")
	}
}
