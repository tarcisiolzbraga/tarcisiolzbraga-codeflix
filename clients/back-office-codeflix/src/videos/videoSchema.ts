import { z } from "zod";
import { RATINGS } from "@/videos/videosGateway";

// Os limites do `VideoValidator` do domínio. Dois deles diferem dos outros agregados, e foram lidos
// de lá em vez de copiados por analogia:
//
// - o **título** vai de 1 a 255, como o nome do gênero — e não de 3, como categoria e elenco;
// - a **descrição é obrigatória**, diferente da categoria, onde ela é opcional. E tem o mesmo teto
//   de 4000.
//
// `duration` não pode ser negativa, e `launchedAt` é obrigatório.
const TITULO_MAX = 255;
const DESCRICAO_MAX = 4000;
const ANO_MIN = 1800;

export const videoSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, "O título é obrigatório.")
    .max(TITULO_MAX, `O título passa de ${TITULO_MAX} caracteres.`),
  description: z
    .string()
    .trim()
    .min(1, "A sinopse é obrigatória.")
    .max(DESCRICAO_MAX, `A sinopse passa de ${DESCRICAO_MAX} caracteres.`),
  // Números, e não texto coagido: quem converte é o `valueAsNumber` do react-hook-form, no
  // `register` do campo. Com `z.coerce` o tipo de **entrada** do esquema vira `unknown`, e o
  // formulário perde a conferência de tipo dos próprios campos.
  //
  // Campo vazio chega como `NaN`, que o `z.number()` recusa — é daí que sai "é obrigatório".
  // O mínimo do ano é piso de digitação, não regra de domínio: catálogo tem filme de 1902, e
  // estreia marcada para o ano que vem é caso real, então não há teto.
  launchedAt: z
    .number({ error: "O ano de lançamento é obrigatório." })
    .int("O ano é um número inteiro.")
    .min(ANO_MIN, `O ano precisa ser ${ANO_MIN} ou mais.`),
  duration: z
    .number({ error: "A duração é obrigatória." })
    .nonnegative("A duração não pode ser negativa."),
  rating: z.enum(RATINGS, { error: "Escolha a classificação indicativa." }),
  categories: z.array(z.string()),
  genres: z.array(z.string()),
  castMembers: z.array(z.string()),
});

export type VideoFormValues = z.infer<typeof videoSchema>;

export const VIDEO_FORM_DEFAULTS: VideoFormValues = {
  title: "",
  description: "",
  launchedAt: new Date().getFullYear(),
  duration: 0,
  rating: "L",
  categories: [],
  genres: [],
  castMembers: [],
};

export function toDraft(values: VideoFormValues) {
  return { ...values, title: values.title.trim(), description: values.description.trim() };
}
