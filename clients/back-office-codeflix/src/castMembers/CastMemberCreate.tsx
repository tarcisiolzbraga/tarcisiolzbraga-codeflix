import { Stack, Typography } from "@mui/material";
import { useNavigate } from "react-router";
import { CastMemberForm } from "@/castMembers/CastMemberForm";
import { toDraft } from "@/castMembers/castMemberSchema";
import { useCreateCastMember } from "@/castMembers/useCastMemberMutations";
import { useFeedback } from "@/shared/feedback";

export function CastMemberCreate() {
  const navegar = useNavigate();
  const avisar = useFeedback();
  const criar = useCreateCastMember();

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Novo membro de elenco</Typography>

      <CastMemberForm
        showActive
        submitLabel="Criar"
        failure={criar.error}
        pending={criar.isPending}
        onSubmit={(values) =>
          criar.mutate(toDraft(values), {
            onSuccess: () => {
              avisar({ kind: "success", message: "Membro de elenco criado." });
              void navegar("/elenco");
            },
          })
        }
      />
    </Stack>
  );
}
