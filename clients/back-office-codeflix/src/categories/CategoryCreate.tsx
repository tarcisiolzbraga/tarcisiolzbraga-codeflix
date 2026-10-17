import { Stack, Typography } from "@mui/material";
import { useNavigate } from "react-router";
import { CategoryForm } from "@/categories/CategoryForm";
import { toDraft } from "@/categories/categorySchema";
import { useCreateCategory } from "@/categories/useCategoryMutations";
import { useFeedback } from "@/shared/feedback";

export function CategoryCreate() {
  const navegar = useNavigate();
  const avisar = useFeedback();
  const criar = useCreateCategory();

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Nova categoria</Typography>

      <CategoryForm
        showActive
        submitLabel="Criar"
        failure={criar.error}
        pending={criar.isPending}
        onSubmit={(values) =>
          // A navegação é no `onSuccess` da chamada, e não logo depois de enviar: sair da tela antes
          // da resposta levaria a pessoa para a lista sem a categoria, e sem saber que o 422 veio.
          criar.mutate(toDraft(values), {
            onSuccess: () => {
              avisar({ kind: "success", message: "Categoria criada." });
              void navegar("/categorias");
            },
          })
        }
      />
    </Stack>
  );
}
