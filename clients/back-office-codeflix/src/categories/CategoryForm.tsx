import { zodResolver } from "@hookform/resolvers/zod";
import {
  Alert,
  AlertTitle,
  Button,
  FormControlLabel,
  Stack,
  Switch,
  TextField,
} from "@mui/material";
import { Controller, useForm } from "react-hook-form";
import { Link } from "react-router";
import {
  CATEGORY_FORM_DEFAULTS,
  categorySchema,
  type CategoryFormValues,
} from "@/categories/categorySchema";
import { AdminApiError } from "@/shared/http";
import { explainFailure } from "@/shared/explainFailure";

// Um formulário para criar e para editar, porque os campos são os mesmos. O que muda é dito por
// props, e não por um `if` lendo a rota: quem renderiza sabe se há id, este arquivo não precisa.
//
// **`active` só aparece na criação.** No `UpdateCategoryRequest` da admin-api o campo não existe —
// ativar e desativar têm rotas próprias —, e mostrar o interruptor na edição prometeria uma mudança
// que a chamada não levaria.
export function CategoryForm({
  initial,
  showActive,
  submitLabel,
  failure,
  pending,
  onSubmit,
}: {
  initial?: CategoryFormValues;
  showActive: boolean;
  submitLabel: string;
  failure: unknown;
  pending: boolean;
  onSubmit: (values: CategoryFormValues) => void;
}) {
  const form = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: initial ?? CATEGORY_FORM_DEFAULTS,
  });

  // A lista inteira do `ApiError`, e não só a primeira mensagem: com nome curto e descrição longa
  // vêm duas, e mostrar uma faria a pessoa corrigir e levar o mesmo 422 de volta.
  const recusas = failure instanceof AdminApiError && failure.errors.length > 0 ? failure.errors : [];

  return (
    <Stack component="form" spacing={3} onSubmit={form.handleSubmit(onSubmit)} noValidate>
      {failure !== null && failure !== undefined && (
        <Alert severity="error">
          {/* Com uma recusa só, ela é a frase. Com várias, a frase é o cabeçalho e a lista traz
              todas — repetir a primeira no título **e** na lista, como fazia a primeira versão
              disto, a mostrava duas vezes. */}
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

      <TextField
        label="Descrição"
        multiline
        minRows={4}
        {...form.register("description")}
        error={form.formState.errors.description !== undefined}
        helperText={form.formState.errors.description?.message}
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
              label="Nasce ativa"
            />
          )}
        />
      )}

      <Stack direction="row" spacing={2}>
        <Button type="submit" variant="contained" loading={pending}>
          {submitLabel}
        </Button>
        <Button component={Link} to="/categorias">
          Cancelar
        </Button>
      </Stack>
    </Stack>
  );
}
