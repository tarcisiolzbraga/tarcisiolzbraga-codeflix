import { Stack, Typography } from "@mui/material";
import { useNavigate } from "react-router";
import { GenreForm } from "@/genres/GenreForm";
import { toDraft } from "@/genres/genreSchema";
import { useCreateGenre } from "@/genres/useGenreMutations";
import { useFeedback } from "@/shared/feedback";

export function GenreCreate() {
  const navegar = useNavigate();
  const avisar = useFeedback();
  const criar = useCreateGenre();

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Novo gênero</Typography>

      <GenreForm
        showActive
        submitLabel="Criar"
        failure={criar.error}
        pending={criar.isPending}
        onSubmit={(values) =>
          criar.mutate(toDraft(values), {
            onSuccess: () => {
              avisar({ kind: "success", message: "Gênero criado." });
              void navegar("/generos");
            },
          })
        }
      />
    </Stack>
  );
}
