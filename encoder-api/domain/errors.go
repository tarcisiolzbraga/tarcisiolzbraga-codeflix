package domain

import "errors"

// Os erros do domínio, declarados para quem chama poder distinguir com errors.Is em vez de comparar
// texto. O que vem de fora — a mensagem da fila — é o que mais erra, então cada motivo tem o seu.
var (
	ErrUnknownMediaType = errors.New("tipo de mídia desconhecido")
	ErrEmptyVideoID     = errors.New("'videoId' não pode ser vazio")
	ErrEmptyChecksum    = errors.New("'checksum' não pode ser vazio")
	ErrEmptyFilePath    = errors.New("'filePath' não pode ser vazio")
	ErrEmptyOutputPath  = errors.New("o caminho de saída não pode ser vazio")
	ErrNoMedia          = errors.New("o trabalho precisa de uma mídia")
	ErrInvalidStatus    = errors.New("transição de status inválida")
)
