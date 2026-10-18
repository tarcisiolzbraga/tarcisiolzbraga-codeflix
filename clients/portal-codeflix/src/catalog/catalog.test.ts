import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  CatalogRefused,
  fetchSignedMediaUrl,
  CatalogUnavailable,
  fetchVideo,
  formatDuration,
  isMissingCatalogRole,
  ratingLabel,
  searchVideos,
} from "@/catalog/catalog";

const video = {
  id: "33333333-3333-3333-3333-333333333333",
  title: "Um filme",
  description: "Uma descrição",
  yearLaunched: 2026,
  duration: 107,
  rating: "14",
  video: "raw/v1.mp4",
  trailer: null,
  banner: "banners/um-filme.png",
  thumbnail: "thumbs/um-filme.png",
  thumbnailHalf: null,
  genres: [{ id: "g1", name: "Ação" }],
  categories: [{ id: "c1", name: "Filmes" }],
  castMembers: [],
};

function respondWith(body: unknown, status = 200) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    Promise.resolve(
      new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }),
    ),
  );
}

describe("searchVideos", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenATokenAndATerm_whenSearches_thenPostsGraphQlToTheCatalogWithTheBearer", async () => {
    // Given o catálogo respondendo uma página
    const fetchSpy = respondWith({ data: { videos: { meta: { currentPage: 0, perPage: 12, total: 1 }, items: [video] } } });

    // When busca
    const actual = await searchVideos({ search: "filme", page: 0 }, "um-token");

    // Then vai por POST para /api/graphql — o contexto /api faz parte do endereço —, com o token e
    // o termo como variável, e não interpolado no documento
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8082/api/graphql");
    expect(new Headers(options?.headers).get("Authorization")).toBe("Bearer um-token");
    expect(JSON.parse(String(options?.body)).variables).toEqual({ search: "filme", page: 0 });
    expect(actual.items[0]?.title).toBe("Um filme");
  });

  it("givenATokenWithoutTheCatalogRole_whenSearches_thenRefusesInsideTheBodyWithHttp200", async () => {
    // Given o GraphQL recusando: **200 de HTTP**, com a classificação no corpo
    respondWith({ errors: [{ message: "Forbidden", extensions: { classification: "FORBIDDEN" } }] }, 200);

    // When busca / Then vira recusa, e não sucesso vazio
    await expect(searchVideos({ search: "", page: 0 }, "um-token")).rejects.toBeInstanceOf(CatalogRefused);
  });

  it("givenAForbiddenRefusal_whenAsksWhy_thenSaysItIsTheMissingRole", async () => {
    // Given a recusa por papel
    respondWith({ errors: [{ message: "Forbidden", extensions: { classification: "FORBIDDEN" } }] });

    // When pergunta o motivo
    const error = await searchVideos({ search: "", page: 0 }, "um-token").catch((it: unknown) => it);

    // Then é o caso de "o papel ainda não chegou ao token", que a tela resolve renovando a sessão —
    // e não "acesso negado" para quem acabou de assinar
    expect(isMissingCatalogRole(error)).toBe(true);
  });

  it("givenAnUnauthorizedRefusal_whenAsksWhy_thenItIsNotTheMissingRole", async () => {
    // Given recusa por falta de identificação
    respondWith({ errors: [{ message: "Unauthorized", extensions: { classification: "UNAUTHORIZED" } }] });

    // When pergunta / Then não é o caso de renovar: é o de entrar
    const error = await searchVideos({ search: "", page: 0 }, "um-token").catch((it: unknown) => it);
    expect(isMissingCatalogRole(error)).toBe(false);
  });

  it("givenATokenThatTheFilterRejects_whenSearches_thenFailsByHttpBeforeGraphQlExists", async () => {
    // Given token inválido: a recusa é do filtro, antes de o GraphQL existir
    respondWith({}, 401);

    // When busca / Then o erro é de transporte, e não de corpo
    await expect(searchVideos({ search: "", page: 0 }, "lixo")).rejects.toBeInstanceOf(CatalogUnavailable);
  });
});

describe("fetchVideo", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenAnIdThatIsNotServedHere_whenFetches_thenReturnsNullInsteadOfFailing", async () => {
    // Given o schema devolvendo nulo, que é resposta normal: "não é servido aqui"
    respondWith({ data: { video: null } });

    // When busca / Then volta null, e a tela diz "não está no acervo"
    await expect(fetchVideo(video.id, "um-token")).resolves.toBeNull();
  });

  // Os campos de arte servem para **uma** coisa: saber se o título tem aquela imagem, antes de pedir
  // o endereço assinado dela. Sem pedi-los, a tela não saberia quando mostrar o substituto — e
  // pediria um endereço para arte que não existe.
  it("givenASearch_whenQueries_thenAsksForTheArtworkFields", async () => {
    // Given uma busca
    const fetching = respondWith({
      data: { videos: { meta: { currentPage: 0, perPage: 12, total: 1 }, items: [video] } },
    });

    // When a consulta é montada
    await searchVideos({ search: "", page: 0 }, "token");

    // Then os três campos de arte estão no documento
    const sent = JSON.parse(String(fetching.mock.calls[0]![1]!.body)) as { query: string };
    expect(sent.query).toContain("banner");
    expect(sent.query).toContain("thumbnail");
    expect(sent.query).toContain("thumbnailHalf");
  });

  it("givenAVideoWithoutArtwork_whenFetches_thenTheFieldsComeNull", async () => {
    // Given um título sem arte nenhuma — o caso de quem acabou de cadastrar
    respondWith({
      data: { video: { ...video, banner: null, thumbnail: null, thumbnailHalf: null } },
    });

    // When o título é buscado
    const found = await fetchVideo(video.id, "token");

    // Then os campos chegam nulos, em vez de ausentes
    expect(found?.banner).toBeNull();
    expect(found?.thumbnail).toBeNull();
  });

  // THUMBNAIL_HALF entrou na união por completude do `VideoMediaType` do admin: a rota do catálogo
  // repassa o tipo como texto, e os cinco valem.
  it("givenTheHalfThumbnail_whenAsksForTheLink_thenTheTypeGoesInTheRoute", async () => {
    // Given um título com meia miniatura
    const fetching = respondWith({ path: "/videos/v1/medias/THUMBNAIL_HALF?x=1", expiresAt: "2026-01-01T00:00:00Z" });

    // When o endereço é pedido
    await fetchSignedMediaUrl("v1", "THUMBNAIL_HALF", "token");

    // Then o tipo vai no caminho
    expect(String(fetching.mock.calls[0]![0])).toContain("/medias/THUMBNAIL_HALF/link");
  });

  it("givenAVideoInTheCatalog_whenFetches_thenReturnsItResolved", async () => {
    // Given o título no acervo
    respondWith({ data: { video } });

    // When busca / Then vem com gêneros e categorias já resolvidos
    const actual = await fetchVideo(video.id, "um-token");
    expect(actual?.genres[0]?.name).toBe("Ação");
  });
});

describe("formatDuration", () => {
  it("givenMinutes_whenFormats_thenReadsLikeACatalogAndNotLikeANumber", () => {
    // Given durações em minutos, como o acervo as guarda
    // When formata / Then sai legível
    expect(formatDuration(107)).toBe("1h 47min");
    expect(formatDuration(47)).toBe("47min");
    expect(formatDuration(120)).toBe("2h");
  });

  it("givenAFractionalDuration_whenFormats_thenRoundsInsteadOfShowingDecimals", () => {
    // Given duração fracionária, que o schema permite (Float)
    // When formata / Then arredonda
    expect(formatDuration(90.4)).toBe("1h 30min");
  });
});

describe("ratingLabel", () => {
  it("givenTheLabelsTheAdminPublishes_whenLabels_thenKeepsTheBrazilianMeaning", () => {
    // Given os rótulos do admin
    // When rotula / Then "L" vira Livre e os numéricos viram "N anos" — sem inventar nomenclatura
    expect(ratingLabel("L")).toBe("Livre");
    expect(ratingLabel("14")).toBe("14 anos");
    expect(ratingLabel("ER")).toBe("Especialmente recomendado");
  });
});

describe("fetchSignedMediaUrl", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenATokenAndAVideo_whenAsksForTheLink_thenCallsTheLinkRouteAndJoinsTheRelativePath", async () => {
    // Given o catálogo emitindo o endereço assinado
    const fetchSpy = respondWith({
      path: "/videos/um-id/medias/VIDEO?expires=99&signature=selo",
      expiresAt: "2026-10-16T12:05:00Z",
    });

    // When pede o endereço
    const actual = await fetchSignedMediaUrl("um-id", "VIDEO", "um-token");

    // Then chama a rota de link **com** o token, e junta o caminho relativo com a base — o caminho
    // vem relativo porque quem sabe o host público do catálogo é quem publica o serviço
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8082/api/videos/um-id/medias/VIDEO/link");
    expect(new Headers(options?.headers).get("Authorization")).toBe("Bearer um-token");
    expect(actual?.url).toBe(
      "http://localhost:8082/api/videos/um-id/medias/VIDEO?expires=99&signature=selo",
    );
  });

  it("givenAVideoThatIsNotThere_whenAsksForTheLink_thenReturnsNull", async () => {
    // Given o catálogo respondendo 404
    respondWith(undefined, 404);

    // When pede o endereço / Then volta null, e a tela diz que não há o que reproduzir
    await expect(fetchSignedMediaUrl("um-id", "VIDEO", "um-token")).resolves.toBeNull();
  });

  it("givenTheCatalogRefusing_whenAsksForTheLink_thenFails", async () => {
    // Given o catálogo recusando por falta de direito
    respondWith(undefined, 403);

    // When pede o endereço / Then estoura, e a tela mostra o recado
    await expect(fetchSignedMediaUrl("um-id", "VIDEO", "um-token")).rejects.toBeInstanceOf(
      CatalogUnavailable,
    );
  });
});
