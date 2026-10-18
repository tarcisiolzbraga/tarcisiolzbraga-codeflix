import { zodResolver } from "@hookform/resolvers/zod";
import {
  Alert,
  AlertTitle,
  Button,
  FormControlLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
} from "@mui/material";
import { Controller, useForm } from "react-hook-form";
import { Link } from "react-router";
import {
  CAST_MEMBER_TYPES,
  CAST_MEMBER_TYPE_LABELS,
} from "@/castMembers/castMembersGateway";
import {
  CAST_MEMBER_FORM_DEFAULTS,
  castMemberSchema,
  type CastMemberFormValues,
} from "@/castMembers/castMemberSchema";
import { explainFailure } from "@/shared/explainFailure";
import { AdminApiError } from "@/shared/http";

// Um formulário para criar e para editar, como no da categoria. O papel é uma escolha fechada, e não
// um campo de texto: a API aceita `ator` em minúsculas, mas aceitar texto livre aqui deixaria a
// pessoa digitar "atriz" e tomar um 422 em inglês por algo que a tela sabia de antemão.
export function CastMemberForm({
  initial,
  showActive,
  submitLabel,
  failure,
  pending,
  onSubmit,
}: {
  initial?: CastMemberFormValues;
  showActive: boolean;
  submitLabel: string;
  failure: unknown;
  pending: boolean;
  onSubmit: (values: CastMemberFormValues) => void;
}) {
  const form = useForm<CastMemberFormValues>({
    resolver: zodResolver(castMemberSchema),
    defaultValues: initial ?? CAST_MEMBER_FORM_DEFAULTS,
  });

  const recusas = failure instanceof AdminApiError && failure.errors.length > 0 ? failure.errors : [];

  return (
    <Stack component="form" spacing={3} onSubmit={form.handleSubmit(onSubmit)} noValidate>
      {failure !== null && failure !== undefined && (
        <Alert severity="error">
          {recusas.length > 1 ? (
            <>
              <AlertTitle>A admin-api recusou por {recusas.length} motivos:</AlertTitle>
              <ul style={{ margin: 0, paddingInlineStart: "1.25rem" }}>
                {recusas.map((recusa) => (
                  <li key={recusa}>{recusa}</li>
                ))}
              </ul>
            </>
          ) : (
            explainFailure(failure)
          )}
        </Alert>
      )}

      <TextField
        label="Nome"
        required
        autoFocus
        {...form.register("name")}
        error={form.formState.errors.name !== undefined}
        helperText={form.formState.errors.name?.message}
      />

      <Controller
        name="type"
        control={form.control}
        render={({ field }) => (
          <TextField {...field} select label="Papel" required sx={{ maxWidth: 240 }}>
            {CAST_MEMBER_TYPES.map((tipo) => (
              <MenuItem key={tipo} value={tipo}>
                {CAST_MEMBER_TYPE_LABELS[tipo]}
              </MenuItem>
            ))}
          </TextField>
        )}
      />

      {showActive && (
        <Controller
          name="active"
          control={form.control}
          render={({ field }) => (
            <FormControlLabel
              control={
                <Switch
                  checked={field.value}
                  onChange={(evento) => field.onChange(evento.target.checked)}
                />
              }
              label="Nasce ativo"
            />
          )}
        />
      )}

      <Stack direction="row" spacing={2}>
        <Button type="submit" variant="contained" loading={pending}>
          {submitLabel}
        </Button>
        <Button component={Link} to="/elenco">
          Cancelar
        </Button>
      </Stack>
    </Stack>
  );
}
