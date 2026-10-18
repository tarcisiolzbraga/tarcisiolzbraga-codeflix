import UploadIcon from "@mui/icons-material/UploadFileOutlined";
import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import { useRef, useState } from "react";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { megabytes } from "@/videos/mediaGateway";
import { MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { useUploadMedia } from "@/videos/useVideoMutations";
import {
  MEDIA_LABELS,
  MEDIA_TYPES,
  type MediaType,
  type VideoDetail,
} from "@/videos/videosGateway";

// Os cinco arquivos de um vídeo, um cartão cada.
//
// **Só existe na edição**, e não na criação: a rota de envio é `/videos/{id}/medias/{type}`, então
// sem o vídeo criado não há onde pôr o arquivo. Mostrar o campo antes seria prometer um envio que a
// API não aceitaria.
//
// O que cada cartão mostra vem do próprio `VideoResponse`: arquivo não enviado chega como campo
// **nulo**, e os de áudio e vídeo trazem um `status` de codificação que os de imagem não têm.
function arquivoDe(video: VideoDetail, type: MediaType) {
  switch (type) {
    case "VIDEO":
      return video.video;
    case "TRAILER":
      return video.trailer;
    case "BANNER":
      return video.banner;
    case "THUMBNAIL":
      return video.thumbnail;
    case "THUMBNAIL_HALF":
      return video.thumbnailHalf;
  }
}

function MediaCard({
  video,
  type,
  onUpload,
  enviando,
}: {
  video: VideoDetail;
  type: MediaType;
  onUpload: (type: MediaType, file: File) => void;
  enviando: MediaType | null;
}) {
  const campo = useRef<HTMLInputElement>(null);
  const arquivo = arquivoDe(video, type);
  const status = arquivo !== null && "status" in arquivo ? arquivo.status : null;

  return (
    <Card variant="outlined" sx={{ minWidth: 240, flex: 1 }}>
      <CardContent>
        <Stack spacing={1.5}>
          <Typography variant="subtitle2">{MEDIA_LABELS[type]}</Typography>

          {arquivo === null ? (
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              Nenhum arquivo enviado.
            </Typography>
          ) : (
            <Stack spacing={1} sx={{ alignItems: "flex-start" }}>
              <Typography variant="body2" sx={{ wordBreak: "break-all" }}>
                {arquivo.name}
              </Typography>
              {/* O status é da codificação, e vem do encoder por fila: um vídeo recém-enviado fica
                  em PENDING até o encoder responder. */}
              {status !== null && <Chip size="small" variant="outlined" label={status} />}
            </Stack>
          )}

          <input
            ref={campo}
            type="file"
            hidden
            aria-label={`Arquivo de ${MEDIA_LABELS[type]}`}
            onChange={(evento) => {
              const escolhido = evento.target.files?.[0];
              if (escolhido !== undefined) {
                onUpload(type, escolhido);
              }
              // Limpa o campo para escolher o **mesmo** arquivo de novo depois de uma falha: sem
              // isso o `change` não dispara na segunda escolha igual.
              evento.target.value = "";
            }}
          />

          <Button
            size="small"
            startIcon={<UploadIcon />}
            loading={enviando === type}
            onClick={() => campo.current?.click()}
          >
            {arquivo === null ? "Enviar" : "Substituir"}
          </Button>
        </Stack>
      </CardContent>
    </Card>
  );
}

export function MediaPanel({ video }: { video: VideoDetail }) {
  const avisar = useFeedback();
  const enviar = useUploadMedia(video.id);
  const [enviando, setEnviando] = useState<MediaType | null>(null);

  return (
    <Stack spacing={2}>
      <Typography variant="h6">Mídias</Typography>

      <Alert severity="info">
        Até {megabytes(MEDIA_MAX_FILE_SIZE)} por arquivo. O vídeo e o trailer entram na fila de
        codificação depois do envio, e o status aparece aqui.
      </Alert>

      <Stack direction="row" spacing={2} sx={{ flexWrap: "wrap", gap: 2 }}>
        {MEDIA_TYPES.map((type) => (
          <MediaCard
            key={type}
            video={video}
            type={type}
            enviando={enviando}
            onUpload={(escolhido, file) => {
              setEnviando(escolhido);
              enviar.mutate(
                { type: escolhido, file },
                {
                  onSuccess: () => {
                    setEnviando(null);
                    avisar({ kind: "success", message: `${MEDIA_LABELS[escolhido]} enviado.` });
                  },
                  onError: (erro) => {
                    setEnviando(null);
                    avisar({ kind: "error", message: explainFailure(erro) });
                  },
                },
              );
            }}
          />
        ))}
      </Stack>
    </Stack>
  );
}
