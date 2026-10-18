import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CATALOG_API } from "@/shared/config";

const session = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => session.current }));

const { Artwork } = await import("@/catalog/Artwork");

function respondWith(body: unknown, status = 200) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    Promise.resolve(
      new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" },
      }),
    ),
  );
}

const signed = { path: "/videos/v1/medias/THUMBNAIL?expires=1&signature=abc", expiresAt: "2026-01-01T00:00:00Z" };

describe("Artwork", () => {
  beforeEach(() => {
    session.current = { user: { access_token: "token" } };
    respondWith(signed);
  });

  // `<img src>` não carrega cabeçalho de Authorization, como `<video src>` não carrega: o browser
  // busca o `src` sozinho, sem token. Por isso o endereço precisa se autorizar sozinho.
  it("givenAVideoWithArtwork_whenRenders_thenAsksTheCatalogForASignedAddress", async () => {
    // Given um título com miniatura
    render(
      <Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />,
    );

    // When o endereço é pedido ao montar
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());

    // Then a chamada é a rota de link, com o bearer
    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    expect(String(url)).toBe(`${CATALOG_API}/api/videos/v1/medias/THUMBNAIL/link`);
    expect((init as RequestInit).headers).toMatchObject({ Authorization: "Bearer token" });
  });

  it("givenASignedAddress_whenItArrives_thenTheImageUsesIt", async () => {
    // Given um endereço assinado
    render(
      <Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />,
    );

    // When a arte carrega
    const image = await screen.findByRole("img", { name: "Arte de Um filme" });

    // Then o `src` é o endereço assinado, com a base do catálogo na frente
    expect(image).toHaveAttribute("src", `${CATALOG_API}/api${signed.path}`);
  });

  // Título sem arte é caso comum — recém-cadastrado não tem nenhuma. Pedir um endereço para ela
  // seria uma viagem garantidamente inútil.
  it("givenAVideoWithoutArtwork_whenRenders_thenDoesNotCallTheCatalog", async () => {
    // Given um título sem miniatura
    render(<Artwork videoId="v1" type="THUMBNAIL" location={null} title="Um filme" />);

    // When a tela monta
    await screen.findByText("U");

    // Then nada foi pedido
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenNoArtwork_whenRenders_thenShowsTheInitialInsteadOfAnEmptyBox", async () => {
    // Given um título sem arte
    render(<Artwork videoId="v1" type="BANNER" location={null} title="Duna" />);

    // Then o lugar da arte mostra a inicial, e não um retângulo vazio
    expect(await screen.findByText("D")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  // Arte que não carrega não derruba a tela: o título continua navegável.
  it("givenTheCatalogFailing_whenAsksForTheAddress_thenFallsBackInsteadOfBreaking", async () => {
    // Given o catálogo respondendo erro
    respondWith({}, 500);
    vi.spyOn(console, "error").mockImplementation(() => {});

    render(<Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />);

    // Then o substituto aparece, e nenhuma imagem quebrada
    expect(await screen.findByText("U")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("givenAVideoThatIsNotThere_whenTheLinkIs404_thenFallsBack", async () => {
    // Given 404 na rota de link
    respondWith({}, 404);

    render(<Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />);

    expect(await screen.findByText("U")).toBeInTheDocument();
  });

  // Sem token não há o que assinar, e a chamada voltaria 401.
  it("givenNobodySignedIn_whenRenders_thenDoesNotCallTheCatalog", async () => {
    // Given ninguém logado
    session.current = { user: null };

    render(<Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />);

    await screen.findByText("U");
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenABanner_whenRenders_thenTheTypeGoesInTheRoute", async () => {
    // Given um banner
    render(<Artwork videoId="v1" type="BANNER" location="banners/v1.png" title="Um filme" />);

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(String(vi.mocked(globalThis.fetch).mock.calls[0]![0])).toContain("/medias/BANNER/link");
  });
});

// O jsdom não tem `IntersectionObserver`, e é justamente por isso que o componente pede na hora
// quando ele não existe — os casos acima provam esse caminho. Aqui ele é posto de pé para provar o
// outro: com observador, a arte **espera** entrar na tela.
describe("Artwork, quando o browser observa a tela", () => {
  // Array comum, e não `vi.hoisted`: nada aqui é içado para antes do módulo — o observador falso é
  // montado no `beforeEach`, não numa fábrica de `vi.mock`.
  let callbacks: IntersectionObserverCallback[] = [];

  beforeEach(() => {
    callbacks = [];
    session.current = { user: { access_token: "token" } };
    respondWith(signed);
    vi.stubGlobal(
      "IntersectionObserver",
      class {
        constructor(callback: IntersectionObserverCallback) {
          callbacks.push(callback);
        }
        observe() {}
        disconnect() {}
      },
    );
  });

  function entraNaTela() {
    for (const callback of callbacks) {
      callback(
        [{ isIntersecting: true } as IntersectionObserverEntry],
        {} as IntersectionObserver,
      );
    }
  }

  // Sem isto, uma tela de trilhas pediria o endereço de dezenas de pôsteres que ninguém chegou a ver.
  it("givenArtworkOffScreen_whenItRenders_thenAsksNothingYet", async () => {
    render(<Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />);

    await screen.findByText("U");
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenArtworkThatScrollsIntoView_whenItDoes_thenAsksForTheAddress", async () => {
    render(<Artwork videoId="v1" type="THUMBNAIL" location="thumbs/v1.png" title="Um filme" />);
    await screen.findByText("U");

    entraNaTela();

    expect(await screen.findByRole("img", { name: "Arte de Um filme" })).toBeInTheDocument();
    expect(globalThis.fetch).toHaveBeenCalledOnce();
  });

  // Mesmo visível, título sem arte não pede nada: a guarda de `location` vale nos dois caminhos.
  it("givenNoArtwork_whenItScrollsIntoView_thenStillAsksNothing", async () => {
    render(<Artwork videoId="v1" type="THUMBNAIL" location={null} title="Um filme" />);
    await screen.findByText("U");

    entraNaTela();

    expect(globalThis.fetch).not.toHaveBeenCalled();
  });
});
