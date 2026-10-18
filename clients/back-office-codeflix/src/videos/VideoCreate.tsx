import { Alert, Stack, Typography } from "@mui/material";
import { useNavigate } from "react-router";
import { useFeedback } from "@/shared/feedback";
import { toDraft } from "@/videos/videoSchema";
import { VideoForm } from "@/videos/VideoForm";
import { useCreateVideo } from "@/videos/useVideoMutations";

export function VideoCreate() {
  const navegar = useNavigate();
  const avisar = useFeedback();
  const criar = useCreateVideo();

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Novo vídeo</Typography>

      {/* O vídeo nasce fechado, não publicado e ativo — e os arquivos só entram depois, porque a
          rota de envio precisa do id. Dizer isso aqui evita a procura por campos que não existem. */}
      <Alert severity="info">
        Os arquivos entram depois de criar: a rota de envio precisa do id do vídeo. O vídeo nasce
        ativo, não publicado e fechado.
      </Alert>

      <VideoForm
        submitLabel="Criar"
        failure={criar.error}
        pending={criar.isPending}
        onSubmit={(values) =>
          criar.mutate(toDraft(values), {
            onSuccess: ({ id }) => {
              avisar({ kind: "success", message: "Vídeo criado. Agora envie os arquivos." });
              // Vai para a edição, e não para a lista: é lá que os arquivos entram, e acabar de
              // criar um vídeo sem mídia é metade do trabalho.
              void navegar(`/videos/${id}`);
            },
          })
        }
      />
    </Stack>
  );
}
