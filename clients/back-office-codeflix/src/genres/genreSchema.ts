import { z } from "zod";

// O limite do `GenreValidator` do domínio: nome de **1** a 255, e não de 3 como na categoria e no
// elenco. O mínimo 1 significa, na prática, "não em branco" — e é por isso que a mensagem aqui fala
// de nome vazio, e não de quantidade de caracteres.
const NOME_MAX = 255;

export const genreSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "O nome é obrigatório.")
    .max(NOME_MAX, `O nome passa de ${NOME_MAX} caracteres.`),
  // Gênero sem categoria é estado válido no domínio: o `CreateGenreCommand` troca ausente por
  // conjunto vazio. Exigir uma aqui seria uma regra que a API não tem.
  categories: z.array(z.string()),
  active: z.boolean(),
});

export type GenreFormValues = z.infer<typeof genreSchema>;

export const GENRE_FORM_DEFAULTS: GenreFormValues = { name: "", categories: [], active: true };

export function toDraft(values: GenreFormValues) {
  return { name: values.name.trim(), categories: values.categories, active: values.active };
}
