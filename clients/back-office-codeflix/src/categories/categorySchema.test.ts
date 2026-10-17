import { describe, expect, it } from "vitest";
import { categorySchema, toDraft } from "@/categories/categorySchema";

function erros(values: unknown): string[] {
  const resultado = categorySchema.safeParse(values);
  return resultado.success ? [] : resultado.error.issues.map((questao) => questao.message);
}

const VALIDO = { name: "Documentário", description: "Histórias reais", active: true };

describe("categorySchema", () => {
  it("givenCategoriaValida_whenConferida_thenPassa", () => {
    expect(erros(VALIDO)).toEqual([]);
  });

  // Os limites são os do CategoryValidator do domínio: 3 a 255, contados depois de aparar.
  it("givenNomeComDoisCaracteres_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "do" })).toEqual(["O nome precisa de ao menos 3 caracteres."]);
  });

  it("givenNomeComTresCaracteres_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, name: "doc" })).toEqual([]);
  });

  // O que a contagem aparada muda: três letras com espaço nas bordas passam, duas não.
  it("givenNomeComEspacoNasBordas_whenConferido_thenContaOAparado", () => {
    expect(erros({ ...VALIDO, name: "  doc  " })).toEqual([]);
    expect(erros({ ...VALIDO, name: "  do  " })).toEqual(["O nome precisa de ao menos 3 caracteres."]);
  });

  it("givenNomeVazio_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "   " })).toEqual(["O nome precisa de ao menos 3 caracteres."]);
  });

  it("givenNomeCom256Caracteres_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, name: "a".repeat(256) })).toEqual(["O nome passa de 255 caracteres."]);
  });

  it("givenNomeCom255Caracteres_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, name: "a".repeat(255) })).toEqual([]);
  });

  // 4000 é o limite da coluna, e desde o conserto no domínio também é regra dele: antes, uma
  // descrição mais longa dava 500 em vez de 422.
  it("givenDescricaoCom4001Caracteres_whenConferida_thenRecusa", () => {
    expect(erros({ ...VALIDO, description: "a".repeat(4001) })).toEqual([
      "A descrição passa de 4000 caracteres.",
    ]);
  });

  it("givenDescricaoCom4000Caracteres_whenConferida_thenPassa", () => {
    expect(erros({ ...VALIDO, description: "a".repeat(4000) })).toEqual([]);
  });

  it("givenDescricaoVazia_whenConferida_thenPassa", () => {
    expect(erros({ ...VALIDO, description: "" })).toEqual([]);
  });

  it("givenNomeCurtoEDescricaoLonga_whenConferidos_thenRecusaOsDois", () => {
    expect(erros({ name: "do", description: "a".repeat(4001), active: true })).toHaveLength(2);
  });
});

describe("toDraft", () => {
  it("givenCamposComEspaco_whenConvertidos_thenVaoAparados", () => {
    expect(toDraft({ name: "  Terror  ", description: "  medo  ", active: true })).toEqual({
      name: "Terror",
      description: "medo",
      active: true,
    });
  });

  // Descrição vazia vai como `null`: na coluna ela é anulável, e gravar "" faria a listagem mostrar
  // uma célula vazia que o filtro `like` ainda alcança.
  it("givenDescricaoEmBranco_whenConvertida_thenViraNull", () => {
    expect(toDraft({ name: "Terror", description: "   ", active: false }).description).toBeNull();
  });
});
