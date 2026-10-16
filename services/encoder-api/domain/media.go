package domain

import (
	"time"

	"github.com/google/uuid"
)

// Media é o arquivo que o admin-codeflix enviou e quer convertido.
//
// Não se chama Video, como no curso, porque um vídeo tem mais de uma: o contrato distingue VIDEO de
// TRAILER, e cada uma é convertida por conta própria. Chamar de vídeo o que é uma das mídias dele
// confundiria as duas coisas no mesmo nome.
//
// Sem anotação de json nem de gorm, também diferente do curso: o domínio não sabe como é
// transportado nem como é guardado. Quem traduz é o pacote de mensagem e o de persistência.
type Media struct {
	// ID é gerado aqui e vira a pasta onde a saída codificada fica.
	ID string
	// VideoID é o id do vídeo no admin-codeflix, que volta igual na resposta.
	VideoID string
	Type    MediaType
	// Checksum volta igual na resposta, e é por ele que o admin descarta resposta atrasada: o
	// caminho do arquivo não distingue um envio do outro, porque reenviar sobrescreve no mesmo
	// lugar. Sem devolvê-lo, o vídeo novo passaria a apontar para a saída do que ele substituiu.
	Checksum string
	// FilePath é onde o arquivo está no armazenamento, como o admin o gravou.
	FilePath  string
	CreatedAt time.Time
}

// NewMedia monta a mídia a partir do que chegou na fila, conferindo o que não pode faltar. O id e a
// data são nossos, não vêm da mensagem.
func NewMedia(videoID string, mediaType MediaType, checksum, filePath string, now time.Time) (*Media, error) {
	media := &Media{
		ID:        uuid.NewString(),
		VideoID:   videoID,
		Type:      mediaType,
		Checksum:  checksum,
		FilePath:  filePath,
		CreatedAt: now,
	}
	if err := media.Validate(); err != nil {
		return nil, err
	}
	return media, nil
}

func (m *Media) Validate() error {
	if m.VideoID == "" {
		return ErrEmptyVideoID
	}
	if _, err := ParseMediaType(m.Type.String()); err != nil {
		return err
	}
	if m.Checksum == "" {
		return ErrEmptyChecksum
	}
	if m.FilePath == "" {
		return ErrEmptyFilePath
	}
	return nil
}
