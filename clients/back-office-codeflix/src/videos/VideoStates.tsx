import { FormControlLabel, Stack, Switch, Typography } from "@mui/material";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { useChangeVideoState } from "@/videos/useVideoMutations";
import type { VideoDetail, VideoStateChange } from "@/videos/videosGateway";

// **Três estados independentes, e seis rotas.** Eles não passam pelo corpo da atualização, e são
// coisas diferentes:
//
// | Estado      | O que significa                                  |
// |-------------|--------------------------------------------------|
// | `active`    | o registro existe no catálogo                    |
// | `published` | o vídeo aparece para quem navega                 |
// | `opened`    | pode ser assistido **sem assinatura**            |
//
// Juntá-los num interruptor só, ou num `PUT` com três campos, apagaria a diferença — e `opened` é a
// que custa caro errar: ela libera o arquivo para quem não paga.
const ESTADOS: {
  campo: keyof Pick<VideoDetail, "active" | "published" | "opened">;
  rotulo: string;
  ajuda: string;
  ligar: VideoStateChange;
  desligar: VideoStateChange;
}[] = [
  {
    campo: "active",
    rotulo: "Ativo",
    ajuda: "o registro existe no catálogo",
    ligar: "activate",
    desligar: "deactivate",
  },
  {
    campo: "published",
    rotulo: "Publicado",
    ajuda: "aparece para quem navega",
    ligar: "publish",
    desligar: "unpublish",
  },
  {
    campo: "opened",
    rotulo: "Aberto",
    ajuda: "pode ser assistido sem assinatura",
    ligar: "open",
    desligar: "close",
  },
];

export function VideoStates({ video }: { video: VideoDetail }) {
  const avisar = useFeedback();
  const mudar = useChangeVideoState();

  return (
    <Stack spacing={1}>
      <Typography variant="h6">Situação</Typography>

      <Stack direction={{ xs: "column", sm: "row" }} spacing={3}>
        {ESTADOS.map((estado) => (
          <FormControlLabel
            key={estado.campo}
            label={`${estado.rotulo} — ${estado.ajuda}`}
            control={
              <Switch
                checked={video[estado.campo]}
                onChange={(evento) =>
                  mudar.mutate(
                    {
                      id: video.id,
                      change: evento.target.checked ? estado.ligar : estado.desligar,
                    },
                    {
                      onSuccess: () =>
                        avisar({
                          kind: "success",
                          message: `${estado.rotulo}: ${evento.target.checked ? "sim" : "não"}.`,
                        }),
                      onError: (erro) =>
                        avisar({ kind: "error", message: explainFailure(erro) }),
                    },
                  )
                }
              />
            }
          />
        ))}
      </Stack>
    </Stack>
  );
}
