import { zodResolver } from "@hookform/resolvers/zod";
import {
  Alert,
  AlertTitle,
  Button,
  MenuItem,
  Stack,
  TextField,
} from "@mui/material";
import { Controller, useForm } from "react-hook-form";
import { Link } from "react-router";
import { explainFailure } from "@/shared/explainFailure";
import { AdminApiError } from "@/shared/http";
import { ReferencePicker } from "@/shared/ReferencePicker";
import { VIDEO_FORM_DEFAULTS, videoSchema, type VideoFormValues } from "@/videos/videoSchema";
import { RATINGS } from "@/videos/videosGateway";

// Os campos do vídeo. **Nenhum dos três estados aparece aqui** — `active`, `published` e `opened`
// têm rotas próprias, e cada um deles é uma decisão separada de salvar o texto. Eles são ações da
// tela de edição.
export function VideoForm({
  initial,
  submitLabel,
  failure,
  pending,
  extra,
  onSubmit,
}: {
  initial?: VideoFormValues;
  submitLabel: string;
  failure: unknown;
  pending: boolean;
  // O que a tela de criação põe no fim do formulário: a escolha dos arquivos. Vem por prop, e não
  // por um `if` lendo a rota — quem renderiza sabe se é criação, este arquivo não precisa.
  extra?: React.ReactNode;
  onSubmit: (values: VideoFormValues) => void;
}) {
  const form = useForm<VideoFormValues>({
    resolver: zodResolver(videoSchema),
    defaultValues: initial ?? VIDEO_FORM_DEFAULTS,
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
        label="Título"
        required
        autoFocus
        {...form.register("title")}
        error={form.formState.errors.title !== undefined}
        helperText={form.formState.errors.title?.message}
      />

      {/* A sinopse é **obrigatória** no vídeo, diferente da descrição da categoria. */}
      <TextField
        label="Sinopse"
        required
        multiline
        minRows={4}
        {...form.register("description")}
        error={form.formState.errors.description !== undefined}
        helperText={form.formState.errors.description?.message}
      />

      <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
        <TextField
          label="Ano de lançamento"
          required
          type="number"
          {...form.register("launchedAt", { valueAsNumber: true })}
          error={form.formState.errors.launchedAt !== undefined}
          helperText={form.formState.errors.launchedAt?.message}
        />
        <TextField
          label="Duração (minutos)"
          required
          type="number"
          slotProps={{ htmlInput: { step: "0.1", min: "0" } }}
          {...form.register("duration", { valueAsNumber: true })}
          error={form.formState.errors.duration !== undefined}
          helperText={form.formState.errors.duration?.message}
        />
        <Controller
          name="rating"
          control={form.control}
          render={({ field }) => (
            <TextField {...field} select label="Classificação" required sx={{ minWidth: 160 }}>
              {RATINGS.map((rating) => (
                <MenuItem key={rating} value={rating}>
                  {rating}
                </MenuItem>
              ))}
            </TextField>
          )}
        />
      </Stack>

      {/* Os três vínculos são opcionais: o `VideoReferenceIds` troca ausente por conjunto vazio, e
          vídeo sem categoria, sem gênero ou sem elenco é estado válido. */}
      <Controller
        name="categories"
        control={form.control}
        render={({ field }) => (
          <ReferencePicker resource="categories" value={field.value} onChange={field.onChange} />
        )}
      />
      <Controller
        name="genres"
        control={form.control}
        render={({ field }) => (
          <ReferencePicker resource="genres" value={field.value} onChange={field.onChange} />
        )}
      />
      <Controller
        name="castMembers"
        control={form.control}
        render={({ field }) => (
          <ReferencePicker resource="cast-members" value={field.value} onChange={field.onChange} />
        )}
      />

      {extra}

      <Stack direction="row" spacing={2}>
        <Button type="submit" variant="contained" loading={pending}>
          {submitLabel}
        </Button>
        <Button component={Link} to="/videos">
          Cancelar
        </Button>
      </Stack>
    </Stack>
  );
}
