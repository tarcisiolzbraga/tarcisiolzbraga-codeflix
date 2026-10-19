import { Alert, Stack, Typography } from "@mui/material";
import { useState } from "react";
import { useNavigate } from "react-router";
import { useFeedback } from "@/shared/feedback";
import { sendable, type ChosenMedia } from "@/videos/chosenMedia";
import { MediaPicker } from "@/videos/MediaPicker";
import { toDraft } from "@/videos/videoSchema";
import { VideoForm } from "@/videos/VideoForm";
import { useUploadQueue } from "@/videos/uploadQueue";
import { useCreateVideo } from "@/videos/useVideoMutations";

export function VideoCreate() {
  const navegar = useNavigate();
  const avisar = useFeedback();
  const criar = useCreateVideo();
  const fila = useUploadQueue();
  const [arquivos, setArquivos] = useState<ChosenMedia>({});

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Novo vídeo</Typography>

      {/* O vídeo nasce fechado, não publicado e ativo. Os arquivos são escolhidos aqui e enviados
          depois da criação, porque a rota de envio precisa do id — e é a fila que faz isso. */}
      <Alert severity="info">
        O vídeo nasce ativo, não publicado e fechado. Os arquivos escolhidos abaixo entram na fila de
        envio assim que ele for criado.
      </Alert>

      <VideoForm
        submitLabel="Criar"
        failure={criar.error}
        pending={criar.isPending}
        extra={<MediaPicker chosen={arquivos} onChange={setArquivos} />}
        onSubmit={(values) =>
          criar.mutate(toDraft(values), {
            onSuccess: ({ id }) => {
              // **A fila é alimentada antes de navegar**, e com o título que a pessoa acabou de
              // digitar: ela precisa aparecer na fila identificada, e buscar o vídeo de novo só para
              // ler o título que já está na mão seria uma ida à rede por nada.
              const prontos = sendable(arquivos);
              if (prontos.length > 0) {
                fila.enqueue(
                  prontos.map((escolhido) => ({
                    videoId: id,
                    videoTitle: values.title,
                    type: escolhido.type,
                    file: escolhido.file,
                  })),
                );
              }
              avisar({
                kind: "success",
                message:
                  prontos.length > 0
                    ? `Vídeo criado. ${prontos.length} arquivo(s) na fila de envio.`
                    : "Vídeo criado. Agora envie os arquivos.",
              });
              // Vai para a edição, e não para a lista: é lá que as mídias aparecem, e é onde quem
              // acabou de criar quer estar.
              void navegar(`/videos/${id}`);
            },
          })
        }
      />
    </Stack>
  );
}
