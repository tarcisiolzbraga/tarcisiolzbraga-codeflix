import { Alert, Box, CircularProgress, Stack, Typography } from "@mui/material";
import { useNavigate, useParams } from "react-router";
import { CategoryForm } from "@/categories/CategoryForm";
import { toDraft } from "@/categories/categorySchema";
import { useCategory, useUpdateCategory } from "@/categories/useCategoryMutations";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";

export function CategoryEdit() {
  const { id = "" } = useParams();
  const navegar = useNavigate();
  const avisar = useFeedback();
  const categoria = useCategory(id);
  const salvar = useUpdateCategory(id);

  if (categoria.isPending) {
    return (
      <Box sx={{ display: "grid", placeItems: "center", minHeight: "40vh" }}>
        <CircularProgress aria-label="carregando a categoria" />
      </Box>
    );
  }

  if (categoria.isError) {
    return <Alert severity="error">{explainFailure(categoria.error)}</Alert>;
  }

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Editar categoria</Typography>

      <CategoryForm
        // A situação não entra aqui: a rota de atualização da admin-api não mexe nela, e o
        // interruptor prometeria uma mudança que a chamada não levaria. Ativar e desativar são
        // ações da lista.
        showActive={false}
        submitLabel="Salvar"
        initial={{
          name: categoria.data.name,
          description: categoria.data.description ?? "",
          active: categoria.data.active,
        }}
        failure={salvar.error}
        pending={salvar.isPending}
        onSubmit={(values) => {
          const { name, description } = toDraft(values);
          salvar.mutate(
            { name, description },
            {
              onSuccess: () => {
                avisar({ kind: "success", message: "Categoria salva." });
                void navegar("/categorias");
              },
            },
          );
        }}
      />
    </Stack>
  );
}
