package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
)

var ErrNotFound = errors.New("não encontrado")

type Repository struct {
	db *DB
}

func NewRepository(db *DB) *Repository {
	return &Repository{db: db}
}

// SaveMedia grava a mídia, ou devolve a que já existe para o mesmo envio.
//
// ON CONFLICT no par (video_id, type, checksum) porque a entrega da fila é ao menos uma vez: o
// relay da saída do admin pode repetir a mensagem, e gravar duas mídias para o mesmo envio criaria
// dois trabalhos para a mesma conversão.
func (r *Repository) SaveMedia(ctx context.Context, media *domain.Media) (*domain.Media, error) {
	const sql = `
		INSERT INTO media (id, video_id, type, checksum, file_path, created_at)
		VALUES ($1, $2, $3, $4, $5, $6)
		ON CONFLICT (video_id, type, checksum) DO NOTHING
		RETURNING id, created_at`

	var id string
	var criadaEm = media.CreatedAt
	err := r.db.pool.QueryRow(ctx, sql,
		media.ID, media.VideoID, media.Type.String(), media.Checksum, media.FilePath, media.CreatedAt,
	).Scan(&id, &criadaEm)

	if errors.Is(err, pgx.ErrNoRows) {
		// Já existia: devolve a de lá, com o id que os trabalhos dela referenciam.
		return r.MediaOfUpload(ctx, media.VideoID, media.Type, media.Checksum)
	}
	if err != nil {
		return nil, fmt.Errorf("%w: gravar mídia: %w", ErrDatabase, err)
	}

	gravada := *media
	gravada.ID = id
	gravada.CreatedAt = criadaEm
	return &gravada, nil
}

// MediaOfUpload acha a mídia de um envio. É a pergunta que evita refazer trabalho: se ela já está
// aqui com um trabalho concluído, a conversão não precisa acontecer de novo.
func (r *Repository) MediaOfUpload(
	ctx context.Context, videoID string, tipo domain.MediaType, checksum string,
) (*domain.Media, error) {
	const sql = `
		SELECT id, video_id, type, checksum, file_path, created_at
		FROM media
		WHERE video_id = $1 AND type = $2 AND checksum = $3`

	var media domain.Media
	var tipoTexto string
	err := r.db.pool.QueryRow(ctx, sql, videoID, tipo.String(), checksum).Scan(
		&media.ID, &media.VideoID, &tipoTexto, &media.Checksum, &media.FilePath, &media.CreatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("%w: buscar mídia: %w", ErrDatabase, err)
	}
	media.Type = domain.MediaType(tipoTexto)
	return &media, nil
}

// SaveJob grava o trabalho novo.
func (r *Repository) SaveJob(ctx context.Context, job *domain.Job) error {
	const sql = `
		INSERT INTO job (id, media_id, output_bucket_path, status, error, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7)`

	if _, err := r.db.pool.Exec(ctx, sql,
		job.ID, job.Media.ID, job.OutputBucketPath, string(job.Status), job.Error, job.CreatedAt, job.UpdatedAt,
	); err != nil {
		return fmt.Errorf("%w: gravar trabalho: %w", ErrDatabase, err)
	}
	return nil
}

// UpdateJob guarda a mudança de status. Só o que muda ao longo da vida do trabalho é escrito: o
// resto é imutável depois de criado.
func (r *Repository) UpdateJob(ctx context.Context, job *domain.Job) error {
	const sql = `UPDATE job SET status = $2, error = $3, updated_at = $4 WHERE id = $1`

	etiqueta, err := r.db.pool.Exec(ctx, sql, job.ID, string(job.Status), job.Error, job.UpdatedAt)
	if err != nil {
		return fmt.Errorf("%w: atualizar trabalho: %w", ErrDatabase, err)
	}
	if etiqueta.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

// LastJobOfMedia devolve o trabalho mais recente da mídia, que é como se sabe se ela já foi
// convertida com sucesso.
func (r *Repository) LastJobOfMedia(ctx context.Context, media *domain.Media) (*domain.Job, error) {
	const sql = `
		SELECT id, output_bucket_path, status, error, created_at, updated_at
		FROM job WHERE media_id = $1 ORDER BY created_at DESC LIMIT 1`

	var job domain.Job
	var status string
	err := r.db.pool.QueryRow(ctx, sql, media.ID).Scan(
		&job.ID, &job.OutputBucketPath, &status, &job.Error, &job.CreatedAt, &job.UpdatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("%w: buscar trabalho: %w", ErrDatabase, err)
	}
	job.Status = domain.JobStatus(status)
	job.Media = media
	return &job, nil
}
