import { z } from "zod";

// O que o formulário confere antes de ir à rede, e **só** isso.
//
// O limite do nome é o do `CategoryValidator` do domínio: 3 a 255, contados depois de aparar. Repetir
// aqui não cria segunda fonte de verdade para a regra — cria um atalho para o caso comum, porque a
// mensagem que volta da API é em inglês (`'name' must be between 3 and 255 characters`) e o painel é
// em português. Quando a regra mudar lá, o que chega é o 422, com a mensagem dela.
//
// O da descrição é o mesmo arranjo, e chegou aqui por um caminho que vale registrar: o domínio
// **não** validava descrição, e a coluna é `VARCHAR(4000)` — uma descrição mais longa ia ao banco e
// voltava como erro de integridade, que a API não mapeia. Medido contra a API de verdade: 500, com
// corpo sem explicação nenhuma. O conserto foi no domínio, que agora recusa com 422; este corte aqui
// é o mesmo atalho que o do nome.
const NOME_MIN = 3;
const NOME_MAX = 255;
const DESCRICAO_MAX = 4000;

export const categorySchema = z.object({
  name: z
    .string()
    .trim()
    .min(NOME_MIN, `O nome precisa de ao menos ${NOME_MIN} caracteres.`)
    .max(NOME_MAX, `O nome passa de ${NOME_MAX} caracteres.`),
  description: z.string().trim().max(DESCRICAO_MAX, `A descrição passa de ${DESCRICAO_MAX} caracteres.`),
  active: z.boolean(),
});

export type CategoryFormValues = z.infer<typeof categorySchema>;

export const CATEGORY_FORM_DEFAULTS: CategoryFormValues = { name: "", description: "", active: true };

// Descrição vazia vai como `null`, e não como string vazia: na coluna ela é anulável, e gravar `""`
// faria a listagem mostrar uma célula vazia que o filtro `like` ainda alcança.
export function toDraft(values: CategoryFormValues) {
  return {
    name: values.name.trim(),
    description: values.description.trim() === "" ? null : values.description.trim(),
    active: values.active,
  };
}
