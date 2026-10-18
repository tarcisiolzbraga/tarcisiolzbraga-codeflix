import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  createGenre,
  deleteGenre,
  genresPath,
  getGenre,
  setGenreActive,
  updateGenre,
} from "@/genres/genresGateway";
import { ADMIN_API } from "@/shared/config";
import { SEARCH_DEFAULTS } from "@/shared/search";
import { jsonResponse } from "@/shared/testing";

describe("genresPath", () => {
  it("givenConsultaPadrao_whenMontada_thenApontaParaOsGenres", () => {
    expect(genresPath(SEARCH_DEFAULTS)).toBe("/genres?page=0&perPage=10&sort=name&dir=asc");
  });
});

describe("as chamadas", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ id: "gen-1" })),
    );
  });

  function chamada() {
    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    return { url, init: init as RequestInit };
  }

  it("givenUmGeneroNovo_whenCriado_thenVaiDePostComAsCategorias", async () => {
    await createGenre({ name: "Ação", categories: ["cat-1", "cat-2"], active: true }, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/genres`);
    expect(chamada().init.method).toBe("POST");
    expect(chamada().init.body).toBe(
      JSON.stringify({ name: "Ação", categories: ["cat-1", "cat-2"], active: true }),
    );
  });

  // Gênero sem categoria manda lista vazia, e não omite o campo: o `CreateGenreRequest` aceita os
  // dois, mas mandar explicitamente é o que a tela quer dizer.
  it("givenGeneroSemCategoria_whenCriado_thenMandaListaVazia", async () => {
    await createGenre({ name: "Ação", categories: [], active: true }, "abc.123");

    expect(JSON.parse(chamada().init.body as string)).toMatchObject({ categories: [] });
  });

  // `active` fora do corpo da atualização, e `categories` **substitui** a lista inteira: é o que o
  // `UpdateGenreCommand` faz do outro lado.
  it("givenUmaEdicao_whenSalva_thenVaiDePutSemActiveEComAListaInteira", async () => {
    await updateGenre("gen-1", { name: "Ação", categories: ["cat-3"] }, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/genres/gen-1`);
    expect(chamada().init.method).toBe("PUT");
    expect(chamada().init.body).toBe(JSON.stringify({ name: "Ação", categories: ["cat-3"] }));
  });

  it("givenEdicaoSemCategoria_whenSalva_thenMandaListaVaziaParaTirarTodas", async () => {
    await updateGenre("gen-1", { name: "Ação", categories: [] }, "abc.123");

    expect(JSON.parse(chamada().init.body as string)).toEqual({ name: "Ação", categories: [] });
  });

  it("givenAtivar_whenChamado_thenVaiNaRotaActivate", async () => {
    await setGenreActive("gen-1", true, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/genres/gen-1/activate`);
  });

  it("givenDesativar_whenChamado_thenVaiNaRotaDeactivate", async () => {
    await setGenreActive("gen-1", false, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/genres/gen-1/deactivate`);
  });

  it("givenUmaRemocao_whenChamada_thenVaiDeDeleteNoRecurso", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(undefined, { status: 204 })),
    );

    await deleteGenre("gen-1", "abc.123");

    expect(chamada().init.method).toBe("DELETE");
  });

  it("givenIdComCaractereEspecial_whenUsado_thenVaiEscapado", async () => {
    await getGenre("gen/1 2", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/genres/gen%2F1%202`);
  });
});
