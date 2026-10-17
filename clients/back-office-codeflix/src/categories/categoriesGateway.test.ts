import { describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import {
  CATEGORY_QUERY_DEFAULTS,
  categoriesPath,
  listCategories,
} from "@/categories/categoriesGateway";

describe("categoriesPath", () => {
  // Os padrões são os mesmos do CategorySearchRequest da admin-api. Divergir aqui faria a primeira
  // página do back-office não ser a primeira página que a API considera padrão.
  it("givenConsultaPadrao_whenMontada_thenLevaPaginaZeroDezPorNomeAsc", () => {
    expect(categoriesPath(CATEGORY_QUERY_DEFAULTS)).toBe(
      "/categories?page=0&perPage=10&sort=name&dir=asc",
    );
  });

  // Mandar `search=` vazio pede à admin-api um filtro que não se quer.
  it("givenBuscaVazia_whenMontada_thenOParametroNaoVai", () => {
    expect(categoriesPath({ ...CATEGORY_QUERY_DEFAULTS, search: "   " })).not.toContain("search");
  });

  it("givenBuscaComEspacoNasBordas_whenMontada_thenVaiAparada", () => {
    expect(categoriesPath({ ...CATEGORY_QUERY_DEFAULTS, search: "  terror " })).toContain(
      "search=terror",
    );
  });

  it("givenBuscaComAcentoEEspaco_whenMontada_thenVaiEscapada", () => {
    expect(categoriesPath({ ...CATEGORY_QUERY_DEFAULTS, search: "ficção científica" })).toContain(
      "search=fic%C3%A7%C3%A3o+cient%C3%ADfica",
    );
  });

  it("givenOutraPaginaEOutraOrdem_whenMontada_thenEntramNaUrl", () => {
    expect(
      categoriesPath({ search: "", page: 3, perPage: 25, sort: "createdAt", dir: "desc" }),
    ).toBe("/categories?page=3&perPage=25&sort=createdAt&dir=desc");
  });
});

describe("listCategories", () => {
  it("givenUmToken_whenLista_thenChamaAAdminApiComEle", async () => {
    const pagina = { currentPage: 0, perPage: 10, total: 0, items: [] };
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify(pagina), { headers: { "Content-Type": "application/json" } }),
      ),
    );

    await expect(listCategories(CATEGORY_QUERY_DEFAULTS, "abc.123")).resolves.toEqual(pagina);

    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    expect(url).toBe(`${ADMIN_API}/categories?page=0&perPage=10&sort=name&dir=asc`);
    expect((init as RequestInit).headers).toMatchObject({ Authorization: "Bearer abc.123" });
  });
});
