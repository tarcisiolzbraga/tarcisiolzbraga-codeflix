import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse } from "@/shared/testing";
import {
  changeVideoState,
  createVideo,
  deleteVideo,
  getVideo,
  updateVideo,
  videosPath,
  VIDEO_FILTERS_EMPTY,
  VIDEO_SEARCH_DEFAULTS,
} from "@/videos/videosGateway";

describe("videosPath", () => {
  // **`sort=title`, e não `name`.** É o padrão do `VideoSearchRequest`, e o único dos quatro
  // agregados que difere — copiar `name` pediria ordenação por uma coluna que não existe.
  it("givenConsultaPadrao_whenMontada_thenOrdenaPorTitle", () => {
    expect(videosPath(VIDEO_SEARCH_DEFAULTS, VIDEO_FILTERS_EMPTY)).toBe(
      "/videos?page=0&perPage=10&sort=title&dir=asc",
    );
  });

  // Os filtros por vínculo são **repetição do mesmo parâmetro**, que é como o Spring os lê num
  // `Set<String>`. Juntar com vírgula daria um id só, inexistente.
  it("givenDoisFiltrosDeCategoria_whenMontados_thenRepeteOParametro", () => {
    const caminho = videosPath(VIDEO_SEARCH_DEFAULTS, {
      ...VIDEO_FILTERS_EMPTY,
      categories: ["cat-1", "cat-2"],
    });

    expect(caminho).toContain("categories=cat-1&categories=cat-2");
    expect(caminho).not.toContain("cat-1%2Ccat-2");
  });

  it("givenOsTresFiltros_whenMontados_thenTodosVaoNaUrl", () => {
    const caminho = videosPath(VIDEO_SEARCH_DEFAULTS, {
      categories: ["cat-1"],
      genres: ["gen-1"],
      castMembers: ["cm-1"],
    });

    expect(caminho).toContain("categories=cat-1");
    expect(caminho).toContain("genres=gen-1");
    expect(caminho).toContain("castMembers=cm-1");
  });

  it("givenSemFiltro_whenMontado_thenNaoSobraEComercial", () => {
    expect(videosPath(VIDEO_SEARCH_DEFAULTS, VIDEO_FILTERS_EMPTY)).not.toContain("&&");
  });
});

describe("as chamadas", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ id: "vid-1" })),
    );
  });

  function chamada() {
    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    return { url, init: init as RequestInit };
  }

  const RASCUNHO = {
    title: "Duna",
    description: "Areia",
    launchedAt: 2021,
    duration: 155,
    rating: "14" as const,
    categories: ["cat-1"],
    genres: [],
    castMembers: [],
  };

  it("givenUmVideoNovo_whenCriado_thenVaiDePostNaColecao", async () => {
    await createVideo(RASCUNHO, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/videos`);
    expect(chamada().init.method).toBe("POST");
    expect(JSON.parse(chamada().init.body as string)).toEqual(RASCUNHO);
  });

  // A classificação viaja como o **rótulo** do `Rating`: "14", e não "AGE_14". O nome da constante
  // Java não existe no JSON.
  it("givenAClassificacao_whenEnviada_thenVaiComoRotulo", async () => {
    await createVideo({ ...RASCUNHO, rating: "18" }, "abc.123");

    expect(JSON.parse(chamada().init.body as string)).toMatchObject({ rating: "18" });
  });

  it("givenUmaEdicao_whenSalva_thenVaiDePutNoRecurso", async () => {
    await updateVideo("vid-1", RASCUNHO, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/videos/vid-1`);
    expect(chamada().init.method).toBe("PUT");
  });

  // As seis rotas de estado, cada uma no seu caminho. Errar uma faria "publicar" despublicar.
  it.each([
    ["publish", "/publish"],
    ["unpublish", "/unpublish"],
    ["open", "/open"],
    ["close", "/close"],
    ["activate", "/activate"],
    ["deactivate", "/deactivate"],
  ] as const)("givenAMudanca_%s_whenChamada_thenVaiNaRotaDela", async (change, cauda) => {
    await changeVideoState("vid-1", change, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/videos/vid-1${cauda}`);
    expect(chamada().init.method).toBe("PUT");
  });

  it("givenUmaRemocao_whenChamada_thenVaiDeDeleteNoRecurso", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(undefined, { status: 204 })),
    );

    await deleteVideo("vid-1", "abc.123");

    expect(chamada().init.method).toBe("DELETE");
  });

  it("givenIdComCaractereEspecial_whenUsado_thenVaiEscapado", async () => {
    await getVideo("vid/1 2", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/videos/vid%2F1%202`);
  });
});
