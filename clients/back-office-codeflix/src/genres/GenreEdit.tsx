import { Alert, Box, CircularProgress, Stack, Typography } from "@mui/material";
import { useNavigate, useParams } from "react-router";
import { GenreForm } from "@/genres/GenreForm";
import { toDraft } from "@/genres/genreSchema";
import { useGenre, useUpdateGenre } from "@/genres/useGenreMutations";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";

export function GenreEdit() {
  const { id = "" } = useParams();
  const navegar = useNavigate();
  const avisar = useFeedback();
  const genero = useGenre(id);
  const salvar = useUpdateGenre(id);

  if (genero.isPending) {
    return (
      <Box sx={{ display: "grid", placeItems: "center", minHeight: "40vh" }}>
        <CircularProgress aria-label="carregando o gênero" />
      </Box>
    );
  }

  if (genero.isError) {
    return <Alert severity="error">{explainFailure(genero.error)}</Alert>;
  }

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Editar gênero</Typography>

      <GenreForm
        showActive={false}
        submitLabel="Salvar"
        initial={{
          name: genero.data.name,
          categories: genero.data.categories,
          active: genero.data.active,
        }}
        failure={salvar.error}
        pending={salvar.isPending}
        onSubmit={(values) => {
          const { name, categories } = toDraft(values);
          salvar.mutate(
            { name, categories },
            {
              onSuccess: () => {
                avisar({ kind: "success", message: "Gênero salvo." });
                void navegar("/generos");
              },
            },
          );
        }}
      />
    </Stack>
  );
}
