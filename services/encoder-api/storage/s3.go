// Package storage conversa com o Garage, que é compatível com S3. O curso usa Google Cloud Storage
// e um arquivo de credencial baixado à mão; aqui o armazenamento já existe no compose, e é o mesmo
// que o admin-codeflix usa para guardar o arquivo enviado.
package storage

import (
	"context"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/s3/types"
)

var (
	ErrNotFound = errors.New("objeto não encontrado no armazenamento")
	ErrStorage  = errors.New("falha no armazenamento")
)

// Config é o que o encoder precisa saber do Garage. Os mesmos valores que o admin usa: bucket e
// credencial são compartilhados, porque é o mesmo armazenamento.
type Config struct {
	Endpoint  string
	Region    string
	Bucket    string
	AccessKey string
	SecretKey string
}

type Store struct {
	client *s3.Client
	bucket string
}

// New monta o cliente apontado para o Garage.
//
// UsePathStyle é obrigatório: o endereço no estilo de subdomínio (bucket.host) depende de DNS que
// não existe aqui dentro, e o pedido iria para um host inexistente.
func New(cfg Config) *Store {
	client := s3.New(s3.Options{
		Region:       cfg.Region,
		BaseEndpoint: aws.String(cfg.Endpoint),
		UsePathStyle: true,
		Credentials:  credentials.NewStaticCredentialsProvider(cfg.AccessKey, cfg.SecretKey, ""),
	})
	return &Store{client: client, bucket: cfg.Bucket}
}

// Download traz o objeto para um arquivo local e devolve o caminho dele.
func (s *Store) Download(ctx context.Context, key, destino string) (string, error) {
	saida, err := s.client.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String(s.bucket),
		Key:    aws.String(key),
	})
	if err != nil {
		if ehAusente(err) {
			return "", fmt.Errorf("%w: %s", ErrNotFound, key)
		}
		return "", fmt.Errorf("%w: baixar %s: %w", ErrStorage, key, err)
	}
	defer saida.Body.Close()

	if err := os.MkdirAll(filepath.Dir(destino), 0o755); err != nil {
		return "", fmt.Errorf("%w: %w", ErrStorage, err)
	}
	arquivo, err := os.Create(destino)
	if err != nil {
		return "", fmt.Errorf("%w: %w", ErrStorage, err)
	}
	defer arquivo.Close()

	if _, err := io.Copy(arquivo, saida.Body); err != nil {
		return "", fmt.Errorf("%w: gravar %s: %w", ErrStorage, destino, err)
	}
	return destino, nil
}

// UploadDir sobe a pasta inteira, preservando a estrutura sob prefix. O pacote DASH é um manifesto
// mais vários segmentos, e eles só servem juntos.
func (s *Store) UploadDir(ctx context.Context, dir, prefix string) error {
	return filepath.WalkDir(dir, func(caminho string, entrada os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if entrada.IsDir() {
			return nil
		}
		relativo, err := filepath.Rel(dir, caminho)
		if err != nil {
			return err
		}
		arquivo, err := os.Open(caminho)
		if err != nil {
			return fmt.Errorf("%w: %w", ErrStorage, err)
		}
		defer arquivo.Close()

		chave := prefix + "/" + filepath.ToSlash(relativo)
		if _, err := s.client.PutObject(ctx, &s3.PutObjectInput{
			Bucket: aws.String(s.bucket),
			Key:    aws.String(chave),
			Body:   arquivo,
		}); err != nil {
			return fmt.Errorf("%w: subir %s: %w", ErrStorage, chave, err)
		}
		return nil
	})
}

// ehAusente distingue "não existe" de "o armazenamento está fora": o primeiro não melhora com nova
// tentativa, e o admin precisa saber a diferença entre arquivo apagado e Garage indisponível.
//
// Pelo tipo que o SDK devolve, e não por texto do erro: mensagem de erro é detalhe de versão e
// muda sem avisar.
func ehAusente(err error) bool {
	var semChave *types.NoSuchKey
	if errors.As(err, &semChave) {
		return true
	}
	var semBucket *types.NoSuchBucket
	return errors.As(err, &semBucket)
}
