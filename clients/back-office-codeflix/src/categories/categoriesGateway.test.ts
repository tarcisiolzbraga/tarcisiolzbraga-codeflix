import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import {
  CATEGORY_QUERY_DEFAULTS,
  categoriesPath,
  createCategory,
  deleteCategory,
  getCategory,
  listCategories,
  setCategoryActive,
  updateCategory,
} from "@/categories/categoriesGateway";
import { jsonResponse } from "@/shared/testing";

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

// As escritas, uma a uma: o que importa em cada é o **verbo** e o **caminho**, porque errar um deles
// não dá erro de compilação — dá 405 ou uma ação que não aconteceu.
describe("as escritas", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ id: "cat-1" })),
    );
  });

  function chamada() {
    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    return { url, init: init as RequestInit };
  }

  it("givenUmaCategoriaNova_whenCriada_thenVaiDePostNaColecao", async () => {
    await createCategory({ name: "Terror", description: "medo", active: true }, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories`);
    expect(chamada().init.method).toBe("POST");
    expect(chamada().init.body).toBe(
      JSON.stringify({ name: "Terror", description: "medo", active: true }),
    );
  });

  // `active` **não** entra no corpo da atualização: no `UpdateCategoryRequest` da admin-api o campo
  // não existe, porque ativar e desativar têm rotas próprias. Mandá-lo seria mandar algo que o outro
  // lado ignora em silêncio.
  it("givenUmaEdicao_whenSalva_thenVaiDePutSemOCampoActive", async () => {
    await updateCategory("cat-1", { name: "Terror", description: null }, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories/cat-1`);
    expect(chamada().init.method).toBe("PUT");
    expect(chamada().init.body).toBe(JSON.stringify({ name: "Terror", description: null }));
  });

  it("givenAtivar_whenChamado_thenVaiNaRotaActivate", async () => {
    await setCategoryActive("cat-1", true, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories/cat-1/activate`);
    expect(chamada().init.method).toBe("PUT");
  });

  it("givenDesativar_whenChamado_thenVaiNaRotaDeactivate", async () => {
    await setCategoryActive("cat-1", false, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories/cat-1/deactivate`);
  });

  it("givenUmaRemocao_whenChamada_thenVaiDeDeleteNoRecurso", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(undefined, { status: 204 })),
    );

    await deleteCategory("cat-1", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories/cat-1`);
    expect(chamada().init.method).toBe("DELETE");
  });

  it("givenUmaBusca_whenPeloId_thenVaiDeGetNoRecurso", async () => {
    await getCategory("cat-1", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories/cat-1`);
    expect(chamada().init.method).toBe("GET");
  });

  // Id vem da URL, e a rota da admin-api o recebe no caminho: um id com barra ou espaço mudaria a
  // rota chamada se não fosse escapado.
  it("givenIdComCaractereEspecial_whenUsado_thenVaiEscapado", async () => {
    await getCategory("cat/1 2", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/categories/cat%2F1%202`);
  });
});
