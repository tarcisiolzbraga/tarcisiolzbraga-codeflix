import { describe, expect, it } from "vitest";
import { genreSchema, toDraft } from "@/genres/genreSchema";

function erros(values: unknown): string[] {
  const resultado = genreSchema.safeParse(values);
  return resultado.success ? [] : resultado.error.issues.map((questao) => questao.message);
}

const VALIDO = { name: "Ação", categories: ["cat-1"], active: true };

describe("genreSchema", () => {
  it("givenGeneroValido_whenConferido_thenPassa", () => {
    expect(erros(VALIDO)).toEqual([]);
  });

  // O mínimo do GenreValidator é **1**, e não 3 como na categoria e no elenco: um nome de duas
  // letras é válido aqui. Copiar o 3 recusaria o que a API aceita.
  it("givenNomeComDuasLetras_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, name: "Fi" })).toEqual([]);
  });

  it("givenNomeEmBranco_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "   " })).toEqual(["O nome é obrigatório."]);
  });

  it("givenNomeCom256Caracteres_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "a".repeat(256) })).toEqual(["O nome passa de 255 caracteres."]);
  });

  // Gênero sem categoria é estado válido no domínio: o CreateGenreCommand troca ausente por conjunto
  // vazio. Exigir uma aqui seria uma regra que a API não tem.
  it("givenSemCategoria_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, categories: [] })).toEqual([]);
  });
});

describe("toDraft", () => {
  it("givenNomeComEspaco_whenConvertido_thenVaiAparado", () => {
    expect(toDraft({ name: "  Ação  ", categories: ["cat-1"], active: false })).toEqual({
      name: "Ação",
      categories: ["cat-1"],
      active: false,
    });
  });
});
