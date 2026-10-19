import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { installXhrDouble, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { Layout } = await import("@/shared/Layout");
const { useUploadQueue } = await import("@/videos/uploadQueue");

// Uma tela de mentira que enfileira ao clicar: a fila vive acima do roteador, e o que este arquivo
// afirma é que ela aparece **de qualquer tela** — que é o que a torna fila.
function Enfileirador() {
  const fila = useUploadQueue();
  return (
    <button
      type="button"
      onClick={() =>
        fila.enqueue([
          {
            videoId: "vid-1",
            videoTitle: "Duna",
            type: "VIDEO",
            file: new File(["x"], "duna.mp4", { type: "video/mp4" }),
          },
        ])
      }
    >
      enfileirar
    </button>
  );
}

function renderiza() {
  return renderWithProviders(<Layout />, { path: "/", route: "/" });
}

describe("Layout", () => {
  beforeEach(() => {
    sessao.atual = {
      user: { profile: { name: "Operador" }, access_token: "abc.123" },
      signoutRedirect: vi.fn(),
    };
  });

  // Quatro seções, uma por agregado, e com link de verdade — para abrir em outra aba funcionar.
  it("givenOMenu_whenRenderizado_thenTrazAsQuatroSecoesComLink", () => {
    renderiza();

    const esperados = {
      Categorias: "/categorias",
      Gêneros: "/generos",
      Elenco: "/elenco",
      Vídeos: "/videos",
    };
    for (const [rotulo, caminho] of Object.entries(esperados)) {
      // Dois drawers coexistem — o temporário e o permanente —, então cada rótulo aparece duas
      // vezes. O que importa é o destino.
      const links = screen.getAllByRole("link", { name: rotulo });
      expect(links.length).toBeGreaterThan(0);
      expect(links[0]).toHaveAttribute("href", caminho);
    }
  });

  // No telefone o menu é retrátil, e o botão que o abre existe. Antes havia uma barra de abas, que
  // custava o espaço vertical que as tabelas precisam.
  it("givenOTelefone_whenRenderizado_thenHaBotaoParaAbrirOMenu", () => {
    renderiza();

    expect(screen.getByRole("button", { name: "Abrir o menu" })).toBeInTheDocument();
  });

  it("givenQuemEstaLogado_whenRenderizado_thenONomeApareceEHaComoSair", () => {
    renderiza();

    expect(screen.getByText("Operador")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sair" })).toBeInTheDocument();
  });


  it("givenNadaNaFila_whenRenderizado_thenOPainelNemApareceu", () => {
    renderiza();

    expect(screen.queryByText(/Envios \(/)).not.toBeInTheDocument();
  });
});

describe("a fila vista do layout", () => {
  beforeEach(() => {
    sessao.atual = {
      user: { profile: { name: "Operador" }, access_token: "abc.123" },
      signoutRedirect: vi.fn(),
    };
  });

  it("givenUmEnvioEnfileiradoPorOutraTela_whenAcontece_thenOPainelDoLayoutOMostra", async () => {
    const xhr = installXhrDouble();
    renderWithProviders(
      <>
        <Layout />
        <Enfileirador />
      </>,
      { path: "/", route: "/" },
    );

    await userEvent.click(screen.getByRole("button", { name: "enfileirar" }));

    expect(await screen.findByText("Envios (1)")).toBeInTheDocument();
    expect(screen.getByText(/duna\.mp4/)).toBeInTheDocument();
    await waitFor(() => expect(xhr.sent).toHaveLength(1));
  });

  // O progresso que o browser reporta chega à barra.
  it("givenOBrowserReportandoProgresso_whenAcontece_thenABarraOMostra", async () => {
    const xhr = installXhrDouble();
    renderWithProviders(
      <>
        <Layout />
        <Enfileirador />
      </>,
      { path: "/", route: "/" },
    );
    await userEvent.click(screen.getByRole("button", { name: "enfileirar" }));
    await waitFor(() => expect(xhr.sent).toHaveLength(1));

    xhr.progress(0, 40, 100);

    const barra = await screen.findByLabelText("Progresso de duna.mp4");
    await waitFor(() => expect(barra).toHaveAttribute("aria-valuenow", "40"));
  });

  // Terminado dá para dispensar; em curso, não — cancelar no meio deixaria o arquivo pela metade no
  // armazenamento.
  it("givenUmEnvioEmCurso_whenOlhaOPainel_thenNaoHaComoDispensar", async () => {
    installXhrDouble();
    renderWithProviders(
      <>
        <Layout />
        <Enfileirador />
      </>,
      { path: "/", route: "/" },
    );
    await userEvent.click(screen.getByRole("button", { name: "enfileirar" }));

    await screen.findByText("Envios (1)");
    expect(screen.queryByRole("button", { name: /Dispensar/ })).not.toBeInTheDocument();
  });

  it("givenUmEnvioTerminado_whenOlhaOPainel_thenDaParaDispensarELimpar", async () => {
    const xhr = installXhrDouble();
    renderWithProviders(
      <>
        <Layout />
        <Enfileirador />
      </>,
      { path: "/", route: "/" },
    );
    await userEvent.click(screen.getByRole("button", { name: "enfileirar" }));
    await waitFor(() => expect(xhr.sent).toHaveLength(1));

    xhr.respond(0, 201);

    expect(await screen.findByRole("button", { name: /Dispensar/ })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Limpar" }));
    await waitFor(() => expect(screen.queryByText("Envios (1)")).not.toBeInTheDocument());
  });

  it("givenUmEnvioQueFalhou_whenOlhaOPainel_thenOMotivoApareceNoLugarDaBarra", async () => {
    const xhr = installXhrDouble();
    renderWithProviders(
      <>
        <Layout />
        <Enfileirador />
      </>,
      { path: "/", route: "/" },
    );
    await userEvent.click(screen.getByRole("button", { name: "enfileirar" }));
    await waitFor(() => expect(xhr.sent).toHaveLength(1));

    xhr.respond(0, 404, JSON.stringify({ message: "Video with ID vid-1 was not found" }));

    expect(await screen.findByText("Video with ID vid-1 was not found")).toBeInTheDocument();
    expect(screen.queryByLabelText("Progresso de duna.mp4")).not.toBeInTheDocument();
  });
});
