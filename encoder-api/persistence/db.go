// Package persistence guarda a mídia recebida e cada tentativa de convertê-la.
//
// Com SQL explícito em vez do gorm do curso: são duas tabelas e um punhado de consultas, e deixar
// o SQL à vista é o mesmo princípio do ddl-auto: validate dos serviços Java — o banco não é
// alterado por adivinhação de framework.
package persistence

import (
	"context"
	"embed"
	"errors"
	"fmt"
	"sort"

	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed migrations/*.sql
var migrations embed.FS

var ErrDatabase = errors.New("falha no banco")

type DB struct {
	pool *pgxpool.Pool
}

func Connect(ctx context.Context, url string) (*DB, error) {
	pool, err := pgxpool.New(ctx, url)
	if err != nil {
		return nil, fmt.Errorf("%w: conectar: %w", ErrDatabase, err)
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("%w: sem resposta: %w", ErrDatabase, err)
	}
	return &DB{pool: pool}, nil
}

func (d *DB) Close() { d.pool.Close() }

// Migrate aplica o que ainda não foi aplicado, na ordem do nome do arquivo.
//
// A tabela de controle faz o papel do flyway_schema_history: sem ela, subir duas vezes tentaria
// criar as tabelas de novo. Cada migration roda dentro de uma transação junto do registro dela,
// para não existir estado onde o SQL passou e o registro não.
func (d *DB) Migrate(ctx context.Context) error {
	if _, err := d.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS schema_history (
			nome       VARCHAR(255) NOT NULL PRIMARY KEY,
			aplicada_em TIMESTAMPTZ NOT NULL DEFAULT now()
		)`); err != nil {
		return fmt.Errorf("%w: tabela de controle: %w", ErrDatabase, err)
	}

	arquivos, err := migrations.ReadDir("migrations")
	if err != nil {
		return fmt.Errorf("%w: ler migrations: %w", ErrDatabase, err)
	}
	nomes := make([]string, 0, len(arquivos))
	for _, a := range arquivos {
		nomes = append(nomes, a.Name())
	}
	sort.Strings(nomes)

	for _, nome := range nomes {
		var jaAplicada bool
		if err := d.pool.QueryRow(ctx,
			`SELECT EXISTS (SELECT 1 FROM schema_history WHERE nome = $1)`, nome).Scan(&jaAplicada); err != nil {
			return fmt.Errorf("%w: consultar histórico: %w", ErrDatabase, err)
		}
		if jaAplicada {
			continue
		}

		sql, err := migrations.ReadFile("migrations/" + nome)
		if err != nil {
			return fmt.Errorf("%w: ler %s: %w", ErrDatabase, nome, err)
		}

		tx, err := d.pool.Begin(ctx)
		if err != nil {
			return fmt.Errorf("%w: abrir transação: %w", ErrDatabase, err)
		}
		if _, err := tx.Exec(ctx, string(sql)); err != nil {
			_ = tx.Rollback(ctx)
			return fmt.Errorf("%w: aplicar %s: %w", ErrDatabase, nome, err)
		}
		if _, err := tx.Exec(ctx, `INSERT INTO schema_history (nome) VALUES ($1)`, nome); err != nil {
			_ = tx.Rollback(ctx)
			return fmt.Errorf("%w: registrar %s: %w", ErrDatabase, nome, err)
		}
		if err := tx.Commit(ctx); err != nil {
			return fmt.Errorf("%w: confirmar %s: %w", ErrDatabase, nome, err)
		}
	}
	return nil
}
