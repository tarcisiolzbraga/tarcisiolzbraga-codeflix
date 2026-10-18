import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders, withMeasuredHeight } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { VideoList } = await import("@/videos/VideoList");

const DUNA = {
  id: "vid-1",
  title: "Duna",
  launchedAt: 2021,
  published: true,
  active: true,
  createdAt: "2026-02-03T10:15:00Z",
};

const RASCUNHO = {
  id: "vid-2",
  title: "Matrix",
  launchedAt: 1999,
  published: false,
  active: false,
  createdAt: "2026-02-04T10:15:00Z",
};

function pagina(items: unknown[]) {
  return { currentPage: 0, perPage: 10, total: items.length, items };
}

// O dublê distingue a listagem de vídeo das buscas de referência que os filtros fazem.
function respondeCom(items: unknown[]) {
  vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    const texto = String(url);
    const metodo = (init as RequestInit | undefined)?.method ?? "GET";
    if (metodo !== "GET") {
      return Promise.resolve(jsonResponse(undefined, { status: 204 }));
    }
    if (texto.includes("/videos?")) {
      return Promise.resolve(jsonResponse(pagina(items)));
    }
    return Promise.resolve(jsonResponse(pagina([])));
  });
}

function chamadasDeVideo() {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.map(([url]) => String(url))
    .filter((url) => url.includes("/videos?"));
}

function escritas() {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.filter(([, init]) => ((init as RequestInit).method ?? "GET") !== "GET");
}

function renderiza() {
  return renderWithProviders(<VideoList />, { path: "/videos", route: "/videos" });
}

describe("VideoList", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    withMeasuredHeight();
    respondeCom([]);
  });

  // **Ordena por `title`**, e não por `name`: é o padrão do `VideoSearchRequest`.
  it("givenConsultaPadrao_whenAbre_thenPedeOrdenadoPorTitle", async () => {
    renderiza();

    await waitFor(() => expect(chamadasDeVideo()).not.toHaveLength(0));
    expect(chamadasDeVideo()[0]).toBe(`${ADMIN_API}/videos?page=0&perPage=10&sort=title&dir=asc`);
  });

  it("givenVideos_whenAbre_thenMostraTituloAnoEAsDuasSituacoes", async () => {
    respondeCom([DUNA, RASCUNHO]);

    renderiza();

    expect(await screen.findByText("Duna")).toBeInTheDocument();
    expect(screen.getByText("2021")).toBeInTheDocument();
    expect(screen.getByText("publicado")).toBeInTheDocument();
    expect(screen.getByText("rascunho")).toBeInTheDocument();
    expect(screen.getByText("ativo")).toBeInTheDocument();
    expect(screen.getByText("inativo")).toBeInTheDocument();
  });

  // Os três estados ficam **fora** da lista: `opened` libera o arquivo para quem não paga, e um
  // interruptor ao lado de "remover" convida ao clique errado.
  it("givenALista_whenAbre_thenNaoOfereceMudarEstadoNaLinha", async () => {
    respondeCom([DUNA]);

    renderiza();

    await screen.findByText("Duna");
    expect(screen.queryByRole("menuitem", { name: "Publicar" })).not.toBeInTheDocument();
    expect(screen.queryByRole("menuitem", { name: "Abrir" })).not.toBeInTheDocument();
    expect(screen.getByRole("menuitem", { name: "Editar" })).toBeInTheDocument();
  });

  it("givenALista_whenAbre_thenOfereceCriarUmNovo", async () => {
    renderiza();

    expect(await screen.findByRole("link", { name: /Novo vídeo/ })).toHaveAttribute(
      "href",
      "/videos/novo",
    );
  });

  // Os filtros por vínculo são só da rota de vídeo.
  it("givenOsFiltros_whenAbre_thenOfereceOsTresVinculos", async () => {
    renderiza();

    expect(await screen.findByLabelText("Filtrar por categorias")).toBeInTheDocument();
    expect(screen.getByLabelText("Filtrar por gêneros")).toBeInTheDocument();
    expect(screen.getByLabelText("Filtrar por elenco")).toBeInTheDocument();
  });

  it("givenUmVideo_whenClicaEmRemover_thenPerguntaAntesEAvisaSobreOsArquivos", async () => {
    respondeCom([DUNA]);

    renderiza();
    await screen.findByText("Duna");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));

    expect(await screen.findByText("Remover o vídeo?")).toBeInTheDocument();
    expect(screen.getByText(/com os arquivos enviados/)).toBeInTheDocument();
    expect(escritas()).toHaveLength(0);
  });

  it("givenAPerguntaAberta_whenConfirma_thenChamaODelete", async () => {
    respondeCom([DUNA]);

    renderiza();
    await screen.findByText("Duna");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));
    await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

    await waitFor(() => expect(escritas()).toHaveLength(1));
    expect(escritas()[0]![0]).toBe(`${ADMIN_API}/videos/vid-1`);
    expect(escritas()[0]![1]!.method).toBe("DELETE");
    expect(await screen.findByText("Vídeo removido.")).toBeInTheDocument();
  });
});
