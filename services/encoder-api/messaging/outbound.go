package messaging

import (
	"encoding/json"
	"fmt"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
)

// As três respostas que o admin-codeflix entende em video.encoded.queue. Do outro lado é uma
// interface selada com @JsonTypeInfo em "status", então é o status que diz qual das três é — e ele
// precisa ser o primeiro campo conceitualmente, ainda que a ordem no JSON não importe.
//
// Os três carregam videoId, type e checksum. O checksum é o que diz de qual envio a resposta fala:
// sem ele, uma resposta atrasada seria aplicada ao arquivo que substituiu o original.
type resposta struct {
	Status      string `json:"status"`
	VideoID     string `json:"videoId"`
	Type        string `json:"type"`
	Checksum    string `json:"checksum"`
	EncodedPath string `json:"encodedPath,omitempty"`
	Message     string `json:"message,omitempty"`
}

// Encode monta o JSON da resposta a partir do trabalho, escolhendo a forma pelo status dele.
//
// omitempty nos dois últimos não é economia: o admin desserializa para um record por status, e
// mandar encodedPath num ERROR seria campo que aquele record não tem.
func Encode(job *domain.Job) ([]byte, error) {
	if job == nil || job.Media == nil {
		return nil, domain.ErrNoMedia
	}

	saida := resposta{
		Status:   string(job.Status),
		VideoID:  job.Media.VideoID,
		Type:     job.Media.Type.String(),
		Checksum: job.Media.Checksum,
	}

	switch job.Status {
	case domain.StatusProcessing:
	case domain.StatusCompleted:
		saida.EncodedPath = job.OutputBucketPath
	case domain.StatusError:
		saida.Message = job.Error
	default:
		return nil, fmt.Errorf("%w: %q", domain.ErrInvalidStatus, job.Status)
	}

	return json.Marshal(saida)
}
