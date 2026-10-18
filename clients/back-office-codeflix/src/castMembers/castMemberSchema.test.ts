import { describe, expect, it } from "vitest";
import { castMemberSchema, toDraft } from "@/castMembers/castMemberSchema";

function erros(values: unknown): string[] {
  const resultado = castMemberSchema.safeParse(values);
  return resultado.success ? [] : resultado.error.issues.map((questao) => questao.message);
}

const VALIDO = { name: "Vin Diesel", type: "ACTOR", active: true };

describe("castMemberSchema", () => {
  it("givenMembroValido_whenConferido_thenPassa", () => {
    expect(erros(VALIDO)).toEqual([]);
  });

  // O limite é o do CastMemberValidator: 3 a 255, contados depois de aparar.
  it("givenNomeComDoisCaracteres_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "Vi" })).toEqual([
      "O nome precisa de ao menos 3 caracteres.",
    ]);
  });

  it("givenNomeComEspacoNasBordas_whenConferido_thenContaOAparado", () => {
    expect(erros({ ...VALIDO, name: "  Vin  " })).toEqual([]);
    expect(erros({ ...VALIDO, name: "  Vi  " })).toEqual([
      "O nome precisa de ao menos 3 caracteres.",
    ]);
  });

  it("givenNomeCom256Caracteres_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "a".repeat(256) })).toEqual(["O nome passa de 255 caracteres."]);
  });

  // Os dois papéis do CastMemberType, e nada além deles. A API aceita minúsculas na entrada, mas a
  // escolha fechada da tela manda sempre a forma que ela devolve.
  it("givenPapelDiretor_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, type: "DIRECTOR" })).toEqual([]);
  });

  it("givenPapelDesconhecido_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, type: "ATRIZ" })).toHaveLength(1);
  });

  it("givenPapelEmMinusculas_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, type: "actor" })).toHaveLength(1);
  });
});

describe("toDraft", () => {
  it("givenNomeComEspaco_whenConvertido_thenVaiAparado", () => {
    expect(toDraft({ name: "  Vin Diesel  ", type: "DIRECTOR", active: false })).toEqual({
      name: "Vin Diesel",
      type: "DIRECTOR",
      active: false,
    });
  });
});
