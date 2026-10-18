import { Alert, Box, CircularProgress, Stack, Typography } from "@mui/material";
import { useNavigate, useParams } from "react-router";
import { CastMemberForm } from "@/castMembers/CastMemberForm";
import { toDraft } from "@/castMembers/castMemberSchema";
import { useCastMember, useUpdateCastMember } from "@/castMembers/useCastMemberMutations";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";

export function CastMemberEdit() {
  const { id = "" } = useParams();
  const navegar = useNavigate();
  const avisar = useFeedback();
  const membro = useCastMember(id);
  const salvar = useUpdateCastMember(id);

  if (membro.isPending) {
    return (
      <Box sx={{ display: "grid", placeItems: "center", minHeight: "40vh" }}>
        <CircularProgress aria-label="carregando o membro de elenco" />
      </Box>
    );
  }

  if (membro.isError) {
    return <Alert severity="error">{explainFailure(membro.error)}</Alert>;
  }

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Editar membro de elenco</Typography>

      <CastMemberForm
        // A situação não entra aqui: a rota de atualização da admin-api não mexe nela. Ativar e
        // desativar são ações da lista.
        showActive={false}
        submitLabel="Salvar"
        initial={{ name: membro.data.name, type: membro.data.type, active: membro.data.active }}
        failure={salvar.error}
        pending={salvar.isPending}
        onSubmit={(values) => {
          const { name, type } = toDraft(values);
          salvar.mutate(
            { name, type },
            {
              onSuccess: () => {
                avisar({ kind: "success", message: "Membro de elenco salvo." });
                void navegar("/elenco");
              },
            },
          );
        }}
      />
    </Stack>
  );
}
