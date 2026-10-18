import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const session = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => session.current }));

const { CatalogScreen } = await import("@/catalog/CatalogScreen");

const video = {
  id: "v1",
  title: "Duna",
  description: "Areia, e muita",
  yearLaunched: 2021,
  duration: 155,
  rating: "14",
  banner: "banners/v1.png",
  thumbnail: "thumbs/v1.png",
  thumbnailHalf: null,
  genres: [],
  categories: [],
  castMembers: [],
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

// Um dublê para as duas conversas desta tela: o GraphQL do acervo, e a rota de link de cada arte.
function respondWith(items: unknown[]) {
  return vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    if (((init as RequestInit | undefined)?.method ?? "GET") === "POST") {
      return Promise.resolve(
        json({ data: { videos: { meta: { currentPage: 0, perPage: 12, total: items.length }, items } } }),
      );
    }
    const type = /\/medias\/([A-Z_]+)\/link$/.exec(String(url))?.[1] ?? "";
    return Promise.resolve(
      json({ path: `/videos/v1/medias/${type}?signature=abc`, expiresAt: "2026-01-01T00:00:00Z" }),
    );
  });
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
    respondWith([video]);
  });

  // O pôster é a miniatura, e não o banner: 2:3 em grade, que é a proporção de cartaz.
  it("givenATitleWithAThumbnail_whenTheGridRenders_thenAsksForTheThumbnailAndNotTheBanner", async () => {
    render(<CatalogScreen />);

    await waitFor(() => expect(linkCalls()).not.toHaveLength(0));
    expect(linkCalls()[0]).toContain("/medias/THUMBNAIL/link");
    expect(linkCalls().some((url) => url.includes("BANNER"))).toBe(false);
  });

  it("givenASignedThumbnail_whenItArrives_thenTheCardShowsTheArtwork", async () => {
    render(<CatalogScreen />);

    expect(await screen.findByRole("img", { name: "Arte de Duna" })).toBeInTheDocument();
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

    await screen.findByRole("img", { name: "Arte de Duna" });
    expect(screen.queryByText("Areia, e muita")).not.toBeInTheDocument();
  });

  it("givenATitleWithoutArtwork_whenTheGridRenders_thenShowsTheInitialAndAsksNothing", async () => {
    respondWith([{ ...video, thumbnail: null }]);

    render(<CatalogScreen />);

    expect(await screen.findByText("D")).toBeInTheDocument();
    expect(linkCalls()).toHaveLength(0);
  });

  it("givenTheTitleData_whenTheCardRenders_thenKeepsYearDurationAndRating", async () => {
    render(<CatalogScreen />);

    expect(await screen.findByText(/2021 · 2h 35min · 14 anos/)).toBeInTheDocument();
  });
});
