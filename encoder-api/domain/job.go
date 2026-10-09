package domain

import (
	"time"

	"github.com/google/uuid"
)

// JobStatus são exatamente os três que o admin-codeflix entende no campo "status" da resposta.
// Diferente do curso, que usa texto livre: aqui o conjunto é fechado porque ele é contrato com o
// outro lado, e um valor fora dele seria mensagem que o admin registra e descarta.
type JobStatus string

const (
	StatusProcessing JobStatus = "PROCESSING"
	StatusCompleted  JobStatus = "COMPLETED"
	StatusError      JobStatus = "ERROR"
)

// Job é uma conversão: a mídia, onde a saída foi parar e em que pé está.
type Job struct {
	ID               string
	OutputBucketPath string
	Status           JobStatus
	Media            *Media
	Error            string
	CreatedAt        time.Time
	UpdatedAt        time.Time
}

// NewJob nasce em PROCESSING, que é também a primeira resposta que o admin recebe: ela move a mídia
// de PENDING e diz que alguém pegou o trabalho.
func NewJob(outputBucketPath string, media *Media, now time.Time) (*Job, error) {
	job := &Job{
		ID:               uuid.NewString(),
		OutputBucketPath: outputBucketPath,
		Status:           StatusProcessing,
		Media:            media,
		CreatedAt:        now,
		UpdatedAt:        now,
	}
	if err := job.Validate(); err != nil {
		return nil, err
	}
	return job, nil
}

// Complete fecha o trabalho com sucesso. Só vale a partir de PROCESSING: um trabalho que já falhou
// não passa a ter dado certo, e concluir duas vezes esconderia erro de controle de fluxo.
func (j *Job) Complete(now time.Time) error {
	if j.Status != StatusProcessing {
		return ErrInvalidStatus
	}
	j.Status = StatusCompleted
	j.UpdatedAt = now
	return nil
}

// Fail registra o motivo junto do status: sem ele a resposta de erro chegaria ao admin sem dizer o
// que houve, e o admin só a registra em log — seria log vazio.
func (j *Job) Fail(reason string, now time.Time) error {
	if j.Status != StatusProcessing {
		return ErrInvalidStatus
	}
	j.Status = StatusError
	j.Error = reason
	j.UpdatedAt = now
	return nil
}

func (j *Job) Validate() error {
	if j.Media == nil {
		return ErrNoMedia
	}
	if err := j.Media.Validate(); err != nil {
		return err
	}
	if j.OutputBucketPath == "" {
		return ErrEmptyOutputPath
	}
	return nil
}
