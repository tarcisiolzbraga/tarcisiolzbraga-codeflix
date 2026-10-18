import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { VideoEdit } = await import("@/videos/VideoEdit");

const DUNA = {
  id: "vid-1",
  title: "Duna",
  description: "Areia, e muita",
  launchedAt: 2021,
  duration: 155,
  rating: "14",
  opened: false,
  published: false,
  active: true,
  categories: ["cat-1"],
  genres: [],
  castMembers: [],
  video: { checksum: "abc", name: "duna.mp4", rawLocation: "raw", encodedLocation: "", status: "PENDING" },
  trailer: null,
  banner: null,
  thumbnail: null,
  thumbnailHalf: null,
  createdAt: "2026-02-03T10:15:00Z",
  updatedAt: "2026-02-04T10:15:00Z",
};

function respondeCom(video: unknown = DUNA, status = 200) {
  vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    const texto = String(url);
    const metodo = (init as RequestInit | undefined)?.method ?? "GET";
    if (metodo === "PUT") {
      return Promise.resolve(jsonResponse(DUNA));
    }
    if (metodo === "POST") {
      return Promise.resolve(jsonResponse({ videoId: "vid-1", type: "TRAILER" }));
    }
    if (/\/videos\/[^/?]+$/.test(texto)) {
      return Promise.resolve(jsonResponse(video, { status }));
    }
    if (/\/categories\/[^?]+$/.test(texto)) {
      return Promise.resolve(jsonResponse({ id: "cat-1", name: "Documentário" }));
    }
    return Promise.resolve(jsonResponse({ currentPage: 0, perPage: 20, total: 0, items: [] }));
  });
}

function renderiza() {
  return renderWithProviders(<VideoEdit />, { path: "/videos/:id", route: "/videos/vid-1" });
}

function escritas(metodo: string) {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.filter(([, init]) => (init as RequestInit).method === metodo);
}

describe("VideoEdit", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    respondeCom();
  });

  it("givenUmVideo_whenAbre_thenCarregaPeloIdDaRota", async () => {
    renderiza();

    expect(await screen.findByDisplayValue("Duna")).toBeInTheDocument();
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(`${ADMIN_API}/videos/vid-1`);
  });

  it("givenUmVideo_whenAbre_thenOsCamposVemPreenchidos", async () => {
    renderiza();

    await screen.findByDisplayValue("Duna");
    expect(screen.getByDisplayValue("Areia, e muita")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2021")).toBeInTheDocument();
    expect(screen.getByDisplayValue("155")).toBeInTheDocument();
  });

  // Os três estados são interruptores próprios, e cada um diz o que significa ao lado: `opened`
  // libera o arquivo para quem não paga, e isso não pode ficar implícito.
  it("givenUmVideo_whenAbre_thenOsTresEstadosAparecemComOQueSignificam", async () => {
    renderiza();

    expect(await screen.findByLabelText(/Ativo — o registro existe no catálogo/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Publicado — aparece para quem navega/)).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Aberto — pode ser assistido sem assinatura/),
    ).toBeInTheDocument();
  });

  it.each([
    [/Publicado/, "publish"],
    [/Aberto/, "open"],
  ])("givenOInterruptor_%s_whenLigado_thenChamaARotaDele", async (rotulo, rota) => {
    renderiza();

    await userEvent.click(await screen.findByLabelText(rotulo));

    await waitFor(() => expect(escritas("PUT")).not.toHaveLength(0));
    expect(escritas("PUT")[0]![0]).toBe(`${ADMIN_API}/videos/vid-1/${rota}`);
  });

  // `active` chega ligado, então clicar nele **desativa**.
  it("givenAtivoLigado_whenDesligado_thenChamaDeactivate", async () => {
    renderiza();

    await userEvent.click(await screen.findByLabelText(/Ativo —/));

    await waitFor(() => expect(escritas("PUT")).not.toHaveLength(0));
    expect(escritas("PUT")[0]![0]).toBe(`${ADMIN_API}/videos/vid-1/deactivate`);
  });

  // Os cinco cartões de mídia, com o que foi enviado e o que não.
  it("givenUmVideoComArquivoDeVideo_whenAbre_thenMostraONomeEOStatus", async () => {
    renderiza();

    expect(await screen.findByText("duna.mp4")).toBeInTheDocument();
    expect(screen.getByText("PENDING")).toBeInTheDocument();
    expect(screen.getAllByText("Nenhum arquivo enviado.")).toHaveLength(4);
  });

  it("givenUmArquivoEscolhido_whenEnviado_thenVaiDePostNaRotaDaMidia", async () => {
    renderiza();

    await screen.findByText("duna.mp4");
    const campo = screen.getByLabelText("Arquivo de Trailer");
    await userEvent.upload(campo, new File(["x"], "trailer.mp4", { type: "video/mp4" }));

    await waitFor(() => expect(escritas("POST")).not.toHaveLength(0));
    expect(escritas("POST")[0]![0]).toBe(`${ADMIN_API}/videos/vid-1/medias/TRAILER`);
    expect(await screen.findByText("Trailer enviado.")).toBeInTheDocument();
  });

  it("givenOsCamposTrocados_whenSalva_thenMandaPutNoRecurso", async () => {
    renderiza();

    const titulo = await screen.findByDisplayValue("Duna");
    await userEvent.clear(titulo);
    await userEvent.type(titulo, "Duna: Parte 2");
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    const put = await waitFor(() => {
      const achada = escritas("PUT").find(([url]) => String(url).endsWith("/videos/vid-1"));
      expect(achada).toBeDefined();
      return achada!;
    });

    expect(JSON.parse((put[1] as RequestInit).body as string)).toMatchObject({
      title: "Duna: Parte 2",
      categories: ["cat-1"],
    });
    expect(await screen.findByText("Vídeo salvo.")).toBeInTheDocument();
  });

  // O corpo da atualização **não** leva os três estados: cada um tem rota própria.
  it("givenOsCampos_whenSalva_thenOCorpoNaoLevaOsEstados", async () => {
    renderiza();

    await screen.findByDisplayValue("Duna");
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    const put = await waitFor(() => {
      const achada = escritas("PUT").find(([url]) => String(url).endsWith("/videos/vid-1"));
      expect(achada).toBeDefined();
      return achada!;
    });

    const corpo = JSON.parse((put[1] as RequestInit).body as string) as Record<string, unknown>;
    expect(corpo).not.toHaveProperty("active");
    expect(corpo).not.toHaveProperty("published");
    expect(corpo).not.toHaveProperty("opened");
  });

  it("givenVideoInexistente_whenAbre_thenMostraAMensagemDaApi", async () => {
    respondeCom({ message: "Video with ID vid-1 was not found" }, 404);

    renderiza();

    expect(await screen.findByText("Video with ID vid-1 was not found")).toBeInTheDocument();
  });
});
