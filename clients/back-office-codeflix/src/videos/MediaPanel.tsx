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
import { useRef } from "react";
import { megabytes } from "@/videos/mediaGateway";
import { MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { useUploadQueue } from "@/videos/uploadQueue";
import {
  MEDIA_LABELS,
  MEDIA_TYPES,
  type MediaType,
  type VideoDetail,
} from "@/videos/videosGateway";

// A cor de cada status da codificação. FAILED é o que motivou a tabela: antes dele existir, todo
// status saía com a mesma aparência, e não havia por que distinguir.
const CHIP_COLORS: Record<string, "default" | "success" | "error"> = {
  PENDING: "default",
  PROCESSING: "default",
  COMPLETED: "success",
  FAILED: "error",
};

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
  onChoose,
  enviando,
}: {
  video: VideoDetail;
  type: MediaType;
  onChoose: (type: MediaType, file: File) => void;
  enviando: boolean;
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
                  em PENDING até o encoder responder.

                  **A cor não é enfeite.** Antes todo status saía igual, e FAILED tinha a mesma
                  aparência de PROCESSING: quem olhava não distinguia "está demorando" de "falhou e
                  não vai voltar". */}
              {status !== null && (
                <Chip
                  size="small"
                  variant="outlined"
                  label={status}
                  color={CHIP_COLORS[status] ?? "default"}
                />
              )}
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
                onChoose(type, escolhido);
              }
              // Limpa o campo para escolher o **mesmo** arquivo de novo depois de uma falha: sem
              // isso o `change` não dispara na segunda escolha igual.
              evento.target.value = "";
            }}
          />

          <Button
            size="small"
            startIcon={<UploadIcon />}
            loading={enviando}
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
  const fila = useUploadQueue();

  // O que já está na fila para este vídeo: é o que deixa o botão em estado de envio sem o painel ter
  // estado próprio. A fila é a fonte, e duplicá-la aqui daria duas verdades.
  const naFila = (type: MediaType) =>
    fila.uploads.some(
      (upload) =>
        upload.videoId === video.id &&
        upload.type === type &&
        (upload.status === "waiting" || upload.status === "sending"),
    );

  return (
    <Stack spacing={2}>
      <Typography variant="h6">Mídias</Typography>

      <Alert severity="info">
        Até {megabytes(MEDIA_MAX_FILE_SIZE)} por arquivo. O envio entra na fila do canto da tela e
        continua se você navegar. O vídeo e o trailer entram na fila de codificação depois, e o status
        aparece aqui.
      </Alert>

      <Stack direction="row" spacing={2} sx={{ flexWrap: "wrap", gap: 2 }}>
        {MEDIA_TYPES.map((type) => (
          <MediaCard
            key={type}
            video={video}
            type={type}
            enviando={naFila(type)}
            onChoose={(escolhido, file) =>
              fila.enqueue([
                { videoId: video.id, videoTitle: video.title, type: escolhido, file },
              ])
            }
          />
        ))}
      </Stack>
    </Stack>
  );
}
