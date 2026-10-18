import { describe, expect, it } from "vitest";
import { SEARCH_DEFAULTS, searchPath, withSearch } from "@/shared/search";

describe("searchPath", () => {
  // Os padrões são os dos quatro `*SearchRequest` da admin-api. Divergir faria a primeira página do
  // painel não ser a primeira que a API considera padrão.
  it("givenConsultaPadrao_whenMontada_thenLevaPaginaZeroDezPorNomeAsc", () => {
    expect(searchPath("categories", SEARCH_DEFAULTS)).toBe(
      "/categories?page=0&perPage=10&sort=name&dir=asc",
    );
  });

  it("givenOutroRecurso_whenMontado_thenEleVaiNoCaminho", () => {
    expect(searchPath("cast-members", SEARCH_DEFAULTS)).toMatch(/^\/cast-members\?/);
  });

  // Mandar `search=` vazio pede à admin-api um filtro que não se quer.
  it("givenBuscaVazia_whenMontada_thenOParametroNaoVai", () => {
    expect(searchPath("categories", { ...SEARCH_DEFAULTS, search: "   " })).not.toContain("search");
  });

  it("givenBuscaComEspacoNasBordas_whenMontada_thenVaiAparada", () => {
    expect(searchPath("categories", { ...SEARCH_DEFAULTS, search: "  terror " })).toContain(
      "search=terror",
    );
  });

  it("givenBuscaComAcentoEEspaco_whenMontada_thenVaiEscapada", () => {
    expect(searchPath("videos", { ...SEARCH_DEFAULTS, search: "ficção científica" })).toContain(
      "search=fic%C3%A7%C3%A3o+cient%C3%ADfica",
    );
  });

  it("givenOutraPaginaEOutraOrdem_whenMontada_thenEntramNaUrl", () => {
    expect(
      searchPath("genres", { search: "", page: 3, perPage: 25, sort: "createdAt", dir: "desc" }),
    ).toBe("/genres?page=3&perPage=25&sort=createdAt&dir=desc");
  });
});

describe("withSearch", () => {
  // Filtrar e continuar na página 4 mostraria "nenhum resultado" num filtro que tem resultados.
  it("givenBuscaNova_whenAplicada_thenVoltaParaAPrimeiraPagina", () => {
    const naQuarta = { ...SEARCH_DEFAULTS, page: 3 };

    expect(withSearch(naQuarta, "terror")).toEqual({ ...naQuarta, search: "terror", page: 0 });
  });

  // A mesma busca precisa devolver **a mesma** consulta, e não uma igual: ela vai na chave do
  // TanStack, e um objeto novo a cada render refaria a chamada sem parar.
  it("givenAMesmaBusca_whenAplicada_thenDevolveAMesmaConsulta", () => {
    const naQuarta = { ...SEARCH_DEFAULTS, search: "terror", page: 3 };

    expect(withSearch(naQuarta, "terror")).toBe(naQuarta);
  });
});
