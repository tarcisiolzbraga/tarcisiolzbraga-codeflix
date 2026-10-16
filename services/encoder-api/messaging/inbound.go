// Package messaging traduz entre o domínio e o que trafega na fila. Fica separado do domínio de
// propósito: é aqui que o formato do outro lado pode mudar sem a regra mudar junto.
package messaging

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
)

// VideoMediaCreated é o aviso que o admin-codeflix publica em video.created.queue quando um
// arquivo VIDEO ou TRAILER é enviado. Imagem não gera aviso: ela não é convertida.
//
// Os nomes dos campos são os do record do outro lado, serializado pelo Jackson. Mudar qualquer um
// deles aqui cala o consumo sem erro visível.
type VideoMediaCreated struct {
	VideoID    string    `json:"videoId"`
	Type       string    `json:"type"`
	FilePath   string    `json:"filePath"`
	Checksum   string    `json:"checksum"`
	OccurredOn time.Time `json:"occurredOn"`
}

// ParseVideoMediaCreated lê o aviso e o transforma em mídia do domínio. Erro aqui é mensagem que
// não dá para entender: quem chama registra e descarta, em vez de devolvê-la à fila para falhar de
// novo para sempre.
func ParseVideoMediaCreated(payload []byte, now time.Time) (*domain.Media, error) {
	var aviso VideoMediaCreated
	if err := json.Unmarshal(payload, &aviso); err != nil {
		return nil, fmt.Errorf("aviso ilegível: %w", err)
	}

	tipo, err := domain.ParseMediaType(aviso.Type)
	if err != nil {
		return nil, err
	}

	return domain.NewMedia(aviso.VideoID, tipo, aviso.Checksum, aviso.FilePath, now)
}
