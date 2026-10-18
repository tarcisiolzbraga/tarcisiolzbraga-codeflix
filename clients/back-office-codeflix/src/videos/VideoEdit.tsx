import { Alert, Box, CircularProgress, Divider, Stack, Typography } from "@mui/material";
import { useNavigate, useParams } from "react-router";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { MediaPanel } from "@/videos/MediaPanel";
import { toDraft } from "@/videos/videoSchema";
import { VideoForm } from "@/videos/VideoForm";
import { VideoStates } from "@/videos/VideoStates";
import { useUpdateVideo, useVideo } from "@/videos/useVideoMutations";

export function VideoEdit() {
  const { id = "" } = useParams();
  const navegar = useNavigate();
  const avisar = useFeedback();
  const video = useVideo(id);
  const salvar = useUpdateVideo(id);

  if (video.isPending) {
    return (
      <Box sx={{ display: "grid", placeItems: "center", minHeight: "40vh" }}>
        <CircularProgress aria-label="carregando o vídeo" />
      </Box>
    );
  }

  if (video.isError) {
    return <Alert severity="error">{explainFailure(video.error)}</Alert>;
  }

  return (
    <Stack spacing={4}>
      <Typography variant="h5">Editar vídeo</Typography>

      {/* A situação e as mídias ficam **fora** do formulário, e salvam sozinhas: cada uma é uma
          chamada própria da API, e juntá-las ao botão "Salvar" faria parecer que nada aconteceu até
          salvar — quando a mudança já foi gravada. */}
      <VideoStates video={video.data} />

      <Divider />

      <MediaPanel video={video.data} />

      <Divider />

      <VideoForm
        submitLabel="Salvar"
        initial={{
          title: video.data.title,
          description: video.data.description,
          launchedAt: video.data.launchedAt,
          duration: video.data.duration,
          rating: video.data.rating,
          categories: video.data.categories,
          genres: video.data.genres,
          castMembers: video.data.castMembers,
        }}
        failure={salvar.error}
        pending={salvar.isPending}
        onSubmit={(values) =>
          salvar.mutate(toDraft(values), {
            onSuccess: () => {
              avisar({ kind: "success", message: "Vídeo salvo." });
              void navegar("/videos");
            },
          })
        }
      />
    </Stack>
  );
}
