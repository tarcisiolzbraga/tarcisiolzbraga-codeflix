import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const session = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => session.current }));

const { VideoScreen } = await import("@/catalog/VideoScreen");

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
  genres: [{ id: "g1", name: "Ficção" }],
  categories: [],
  castMembers: [],
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function respondWith(found: unknown) {
  return vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    if (((init as RequestInit | undefined)?.method ?? "GET") === "POST") {
      return Promise.resolve(json({ data: { video: found } }));
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

describe("VideoScreen", () => {
  beforeEach(() => {
    session.current = { isLoading: false, isAuthenticated: true, user: { access_token: "token" } };
    respondWith(video);
  });

  // Na página o cabeçalho é o **banner**, deitado, e não a miniatura.
  it("givenATitleWithABanner_whenThePageRenders_thenAsksForTheBanner", async () => {
    render(<VideoScreen id="v1" />);

    await waitFor(() => expect(linkCalls()).not.toHaveLength(0));
    expect(linkCalls()[0]).toContain("/medias/BANNER/link");
  });

  it("givenASignedBanner_whenItArrives_thenTheHeaderShowsIt", async () => {
    render(<VideoScreen id="v1" />);

    expect(await screen.findByRole("img", { name: "Arte de Duna" })).toBeInTheDocument();
  });

  // O título vem **sobre** o banner, e continua sendo o cabeçalho da página: um `h1` dentro de uma
  // imagem decorativa não é o mesmo que uma imagem com texto dentro.
  it("givenTheBanner_whenItRenders_thenTheTitleIsStillTheHeading", async () => {
    render(<VideoScreen id="v1" />);

    expect(await screen.findByRole("heading", { level: 1, name: "Duna" })).toBeInTheDocument();
  });

  // O endereço da arte é pedido ao montar; o do **player**, só no clique — a assinatura vale cinco
  // minutos, e pedi-la antes faria vencer na mão de quem leu a sinopse com calma.
  it("givenThePageOpens_whenNobodyClicksPlay_thenNoVideoAddressIsAsked", async () => {
    render(<VideoScreen id="v1" />);

    await screen.findByRole("img", { name: "Arte de Duna" });
    expect(linkCalls().some((url) => url.includes("/medias/VIDEO/link"))).toBe(false);
  });

  it("givenATitleWithoutABanner_whenThePageRenders_thenShowsTheInitialAndAsksNothing", async () => {
    respondWith({ ...video, banner: null });

    render(<VideoScreen id="v1" />);

    expect(await screen.findByText("D")).toBeInTheDocument();
    expect(linkCalls()).toHaveLength(0);
  });

  it("givenATitle_whenThePageRenders_thenKeepsTheSynopsisAndTheFacts", async () => {
    render(<VideoScreen id="v1" />);

    expect(await screen.findByText("Areia, e muita")).toBeInTheDocument();
    expect(screen.getByText("Ficção")).toBeInTheDocument();
  });

  // Nulo é resposta normal do schema: "o vídeo não é servido aqui".
  it("givenAnIdThatIsNotServedHere_whenThePageRenders_thenSaysItIsNotInTheCatalog", async () => {
    respondWith(null);

    render(<VideoScreen id="v1" />);

    expect(await screen.findByText("Este título não está no acervo.")).toBeInTheDocument();
  });
});
