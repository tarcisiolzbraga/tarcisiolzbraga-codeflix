package domain

import "fmt"

// MediaType é o que o admin-codeflix manda no campo "type" da mensagem. São só estes dois: imagem
// não é convertida, e por isso o admin nem publica aviso para ela.
type MediaType string

const (
	MediaTypeVideo   MediaType = "VIDEO"
	MediaTypeTrailer MediaType = "TRAILER"
)

// ParseMediaType converte no limite: tipo que não conhecemos não vira MediaType, vira erro. Sem
// isso, um valor novo do outro lado viraria conversão silenciosamente errada em vez de falha.
func ParseMediaType(value string) (MediaType, error) {
	switch MediaType(value) {
	case MediaTypeVideo:
		return MediaTypeVideo, nil
	case MediaTypeTrailer:
		return MediaTypeTrailer, nil
	default:
		return "", fmt.Errorf("%w: %q", ErrUnknownMediaType, value)
	}
}

func (t MediaType) String() string {
	return string(t)
}
