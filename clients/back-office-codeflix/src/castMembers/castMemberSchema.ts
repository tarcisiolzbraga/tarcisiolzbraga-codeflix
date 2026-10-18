import { z } from "zod";
import { CAST_MEMBER_TYPES } from "@/castMembers/castMembersGateway";

// Os limites do `CastMemberValidator` do domínio: nome de 3 a 255, contados depois de aparar, e o
// papel tem de ser um dos dois. O porquê de repetir é o mesmo da categoria — atalho para o caso
// comum, porque a mensagem da API é em inglês. Quando a regra mudar lá, o que chega é o 422.
const NOME_MIN = 3;
const NOME_MAX = 255;

export const castMemberSchema = z.object({
  name: z
    .string()
    .trim()
    .min(NOME_MIN, `O nome precisa de ao menos ${NOME_MIN} caracteres.`)
    .max(NOME_MAX, `O nome passa de ${NOME_MAX} caracteres.`),
  type: z.enum(CAST_MEMBER_TYPES),
  active: z.boolean(),
});

export type CastMemberFormValues = z.infer<typeof castMemberSchema>;

export const CAST_MEMBER_FORM_DEFAULTS: CastMemberFormValues = {
  name: "",
  type: "ACTOR",
  active: true,
};

export function toDraft(values: CastMemberFormValues) {
  return { name: values.name.trim(), type: values.type, active: values.active };
}
