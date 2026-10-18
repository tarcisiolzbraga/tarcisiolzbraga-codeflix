import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, AlertTitle, Button, FormControlLabel, Stack, Switch, TextField } from "@mui/material";
import { Controller, useForm } from "react-hook-form";
import { Link } from "react-router";
import { ReferencePicker } from "@/shared/ReferencePicker";
import { GENRE_FORM_DEFAULTS, genreSchema, type GenreFormValues } from "@/genres/genreSchema";
import { explainFailure } from "@/shared/explainFailure";
import { AdminApiError } from "@/shared/http";

export function GenreForm({
  initial,
  showActive,
  submitLabel,
  failure,
  pending,
  onSubmit,
}: {
  initial?: GenreFormValues;
  showActive: boolean;
  submitLabel: string;
  failure: unknown;
  pending: boolean;
  onSubmit: (values: GenreFormValues) => void;
}) {
  const form = useForm<GenreFormValues>({
    resolver: zodResolver(genreSchema),
    defaultValues: initial ?? GENRE_FORM_DEFAULTS,
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

      {/* Gênero sem categoria é estado válido no domínio, então o campo não é obrigatório. A lista
          escolhida **substitui** a anterior ao salvar, e não acrescenta — é o que a rota faz. */}
      <Controller
        name="categories"
        control={form.control}
        render={({ field }) => <ReferencePicker resource="categories" value={field.value} onChange={field.onChange} />}
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
        <Button component={Link} to="/generos">
          Cancelar
        </Button>
      </Stack>
    </Stack>
  );
}
