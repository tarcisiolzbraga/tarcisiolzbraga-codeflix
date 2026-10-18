import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const session = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => session.current }));

const { CatalogScreen } = await import("@/catalog/CatalogScreen");

const duna = {
  id: "v1",
  title: "Duna",
  description: "Areia, e muita",
  yearLaunched: 2021,
  duration: 155,
  rating: "14",
  video: "raw/v1.mp4",
  trailer: null,
  banner: "banners/v1.png",
  thumbnail: "thumbs/v1.png",
  thumbnailHalf: null,
  genres: [],
  categories: [],
  castMembers: [],
};

const matrix = { ...duna, id: "v2", title: "Matrix", thumbnail: "thumbs/v2.png" };

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

// O dublê distingue as três conversas desta tela pelo **nome da operação** do documento: a busca, os
// gêneros e as trilhas vão todas para `/api/graphql`, e só o nome as separa.
function respondWith({
  found = [duna],
  genres = [{ id: "g1", name: "Ficção" }],
  rails = { t0: { items: [duna, matrix] } } as Record<string, { items: unknown[] }>,
}: {
  found?: unknown[];
  genres?: { id: string; name: string }[];
  rails?: Record<string, { items: unknown[] }>;
} = {}) {
  return vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    if (((init as RequestInit | undefined)?.method ?? "GET") === "POST") {
      const sent = JSON.parse(String((init as RequestInit).body)) as { query: string };
      if (sent.query.includes("query Generos")) {
        return Promise.resolve(json({ data: { genres: { items: genres } } }));
      }
      if (sent.query.includes("query Trilhas")) {
        return Promise.resolve(json({ data: rails }));
      }
      return Promise.resolve(
        json({
          data: { videos: { meta: { currentPage: 0, perPage: 12, total: found.length }, items: found } },
        }),
      );
    }
    const type = /\/medias\/([A-Z_]+)\/link$/.exec(String(url))?.[1] ?? "";
    return Promise.resolve(
      json({ path: `/videos/v1/medias/${type}?signature=abc`, expiresAt: "2026-01-01T00:00:00Z" }),
    );
  });
}

function documentos() {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === "POST")
    .map(([, init]) => (JSON.parse(String((init as RequestInit).body)) as { query: string }).query);
}

function linkCalls() {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.map(([url]) => String(url))
    .filter((url) => url.endsWith("/link"));
}

describe("CatalogScreen", () => {
  beforeEach(() => {
    session.current = { isLoading: false, isAuthenticated: true, user: { access_token: "token" } };
    respondWith();
  });

  // Quem chega sem termo quer descobrir, e trilha por gênero é como se descobre.
  it("givenNobodySearched_whenTheScreenOpens_thenShowsTheGenreRails", async () => {
    render(<CatalogScreen />);

    expect(await screen.findByRole("heading", { level: 2, name: "Ficção" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 3, name: "Duna" })).toBeInTheDocument();
  });

  // **Seis trilhas são um documento, e não seis requisições.** É um dos poucos lugares onde a
  // escolha do catálogo pelo GraphQL se paga de verdade.
  it("givenThreeGenres_whenTheRailsLoad_thenOneDocumentAsksForAllOfThem", async () => {
    respondWith({
      genres: [
        { id: "g1", name: "Ficção" },
        { id: "g2", name: "Terror" },
        { id: "g3", name: "Drama" },
      ],
      rails: {
        t0: { items: [duna] },
        t1: { items: [matrix] },
        t2: { items: [duna] },
      },
    });

    render(<CatalogScreen />);

    await screen.findByRole("heading", { level: 2, name: "Drama" });
    const trilhas = documentos().filter((documento) => documento.includes("query Trilhas"));
    expect(trilhas).toHaveLength(1);
    expect(trilhas[0]).toContain("t0: videos");
    expect(trilhas[0]).toContain("t2: videos");
  });

  // Os ids vão como **variáveis**, nunca interpolados no texto: id vem de fora, e consulta montada
  // por concatenação de valor é como se escreve injeção.
  it("givenTheGenreIds_whenTheRailsAreBuilt_thenTheyTravelAsVariablesAndNotInTheText", async () => {
    render(<CatalogScreen />);

    await screen.findByRole("heading", { level: 2, name: "Ficção" });
    const chamada = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(
        ([, init]) =>
          (init as RequestInit | undefined)?.method === "POST" &&
          String((init as RequestInit).body).includes("query Trilhas"),
      )!;
    const enviado = JSON.parse(String((chamada[1] as RequestInit).body)) as {
      query: string;
      variables: Record<string, unknown>;
    };
    expect(enviado.variables.g0).toBe("g1");
    expect(enviado.query).not.toContain("g1");
  });

  // Gênero sem título publicado existe no acervo, e uma linha com cabeçalho e nada embaixo parece
  // defeito.
  it("givenAGenreWithNoPublishedTitle_whenTheRailsLoad_thenItsRailIsNotShown", async () => {
    respondWith({
      genres: [
        { id: "g1", name: "Ficção" },
        { id: "g2", name: "Vazio" },
      ],
      rails: { t0: { items: [duna] }, t1: { items: [] } },
    });

    render(<CatalogScreen />);

    await screen.findByRole("heading", { level: 2, name: "Ficção" });
    expect(screen.queryByRole("heading", { level: 2, name: "Vazio" })).not.toBeInTheDocument();
  });

  // Quem digitou um termo quer o que casa com ele; trilha ali esconderia resultado atrás de rolagem
  // horizontal.
  it("givenATermTyped_whenItSearches_thenShowsTheGridInsteadOfTheRails", async () => {
    render(<CatalogScreen />);
    await screen.findByRole("heading", { level: 2, name: "Ficção" });

    await userEvent.type(screen.getByPlaceholderText("Buscar no acervo"), "duna");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    await waitFor(() =>
      expect(screen.queryByRole("heading", { level: 2, name: "Ficção" })).not.toBeInTheDocument(),
    );
    expect(screen.getByText("1 título")).toBeInTheDocument();
  });

  // As trilhas são a vitrine de quem chega sem termo; refazê-las ao digitar seria trabalho jogado
  // fora.
  it("givenTheRailsAlreadyLoaded_whenSomeoneSearches_thenTheyAreNotLoadedAgain", async () => {
    render(<CatalogScreen />);
    await screen.findByRole("heading", { level: 2, name: "Ficção" });

    await userEvent.type(screen.getByPlaceholderText("Buscar no acervo"), "duna");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    await waitFor(() => expect(screen.getByText("1 título")).toBeInTheDocument());
    expect(documentos().filter((d) => d.includes("query Trilhas"))).toHaveLength(1);
  });

  // Trilha que não carrega não tira o acervo do ar.
  it("givenTheRailsFailing_whenTheScreenOpens_thenFallsBackToTheGrid", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
      if (((init as RequestInit | undefined)?.method ?? "GET") === "POST") {
        const sent = JSON.parse(String((init as RequestInit).body)) as { query: string };
        if (sent.query.includes("query Generos")) {
          return Promise.resolve(json({}, 500));
        }
        return Promise.resolve(
          json({ data: { videos: { meta: { currentPage: 0, perPage: 12, total: 1 }, items: [duna] } } }),
        );
      }
      return Promise.resolve(json({ path: "/x", expiresAt: "2026-01-01T00:00:00Z" }));
    });

    render(<CatalogScreen />);

    expect(await screen.findByText("1 título")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 3, name: "Duna" })).toBeInTheDocument();
  });

  // O pôster é a miniatura, e não o banner: 2:3 em grade, que é a proporção de cartaz.
  it("givenATitleWithAThumbnail_whenTheCardRenders_thenAsksForTheThumbnailAndNotTheBanner", async () => {
    render(<CatalogScreen />);

    await waitFor(() => expect(linkCalls()).not.toHaveLength(0));
    expect(linkCalls()[0]).toContain("/medias/THUMBNAIL/link");
    expect(linkCalls().some((url) => url.includes("BANNER"))).toBe(false);
  });

  it("givenATitle_whenTheCardRenders_thenItLinksToTheTitlePage", async () => {
    render(<CatalogScreen />);

    const link = await screen.findByRole("link", { name: /Duna/ });
    expect(link).toHaveAttribute("href", "/acervo/v1");
  });

  // A sinopse saiu do cartão: numa grade de pôsteres ela é ruído, e a página do título já a mostra
  // inteira.
  it("givenAGridOfPosters_whenItRenders_thenTheSynopsisIsNotOnTheCard", async () => {
    render(<CatalogScreen />);

    await screen.findByRole("heading", { level: 3, name: "Duna" });
    expect(screen.queryByText("Areia, e muita")).not.toBeInTheDocument();
  });

  it("givenTheTitleData_whenTheCardRenders_thenKeepsYearDurationAndRating", async () => {
    render(<CatalogScreen />);

    expect(await screen.findAllByText(/2021 · 2h 35min · 14 anos/)).not.toHaveLength(0);
  });
});
