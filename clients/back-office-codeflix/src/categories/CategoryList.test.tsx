import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders, withMeasuredHeight } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CategoryList } = await import("@/categories/CategoryList");

function respondeCom(body: unknown, init: ResponseInit = {}) {
  vi.spyOn(globalThis, "fetch").mockImplementation(() => Promise.resolve(jsonResponse(body, init)));
}

function pagina(items: unknown[], total = items.length) {
  return { currentPage: 0, perPage: 10, total, items };
}

const DOCUMENTARIO = {
  id: "cat-1",
  name: "Documentário",
  description: "Histórias reais",
  active: true,
  createdAt: "2026-02-03T10:15:00Z",
};

const ARQUIVADA = {
  id: "cat-2",
  name: "Clássicos",
  description: null,
  active: false,
  createdAt: "2026-02-04T10:15:00Z",
};

function renderiza() {
  return renderWithProviders(<CategoryList />, { path: "/categorias", route: "/categorias" });
}

describe("CategoryList", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    withMeasuredHeight();
    respondeCom(pagina([]));
  });

  it("givenConsultaPadrao_whenAbre_thenPedeAPrimeiraPaginaOrdenadaPorNome", async () => {
    renderiza();

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(
      `${ADMIN_API}/categories?page=0&perPage=10&sort=name&dir=asc`,
    );
  });

  it("givenCategorias_whenAbre_thenMostraNomeDescricaoESituacao", async () => {
    respondeCom(pagina([DOCUMENTARIO, ARQUIVADA]));

    renderiza();

    expect(await screen.findByText("Documentário")).toBeInTheDocument();
    expect(screen.getByText("Histórias reais")).toBeInTheDocument();
    expect(screen.getByText("ativa")).toBeInTheDocument();
    expect(screen.getByText("Clássicos")).toBeInTheDocument();
    expect(screen.getByText("inativa")).toBeInTheDocument();
  });

  it("givenDataEmISO_whenMostrada_thenSaiNoFormatoBrasileiro", async () => {
    respondeCom(pagina([DOCUMENTARIO]));

    renderiza();

    expect(await screen.findByText(/03\/02\/2026/)).toBeInTheDocument();
  });

  // Que a falha chega à tela, e com o motivo certo. O mapa de status para frase é do
  // explainFailure, e tem teste próprio — inclusive o da admin-api fora do ar, que aqui custaria as
  // repetições do TanStack em tempo real.
  it("given401_whenAbre_thenDizQueASessaoVenceu", async () => {
    respondeCom(undefined, { status: 401 });

    renderiza();

    expect(await screen.findByText(/sessão expirou/)).toBeInTheDocument();
  });

  it("given403_whenAbre_thenDizQueFaltaPermissao", async () => {
    respondeCom(undefined, { status: 403 });

    renderiza();

    expect(await screen.findByText(/não tem permissão/)).toBeInTheDocument();
  });

  it("givenMensagemDaAdminApi_whenAbre_thenEhElaQueAparece", async () => {
    respondeCom({ message: "Parâmetro 'dir' inválido" }, { status: 422 });

    renderiza();

    expect(await screen.findByText("Parâmetro 'dir' inválido")).toBeInTheDocument();
  });

  // Chamar sem token voltaria 401 garantido; o `enabled` da consulta evita a ida.
  it("givenSessaoSemToken_whenAbre_thenNemChamaAAdminApi", async () => {
    sessao.atual = { user: null };

    renderiza();

    expect(await screen.findByText("Categorias")).toBeInTheDocument();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenALista_whenAbre_thenOfereceCriarUmaNova", async () => {
    renderiza();

    const botao = await screen.findByRole("link", { name: /Nova categoria/ });
    expect(botao).toHaveAttribute("href", "/categorias/nova");
  });

  describe("as ações da linha", () => {
    beforeEach(() => {
      vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) => {
        const metodo = (init as RequestInit | undefined)?.method ?? "GET";
        return Promise.resolve(
          metodo === "GET"
            ? jsonResponse(pagina([DOCUMENTARIO, ARQUIVADA], 2))
            : jsonResponse(undefined, { status: 204 }),
        );
      });
    });

    function chamadasDeEscrita() {
      return vi
        .mocked(globalThis.fetch)
        .mock.calls.filter(([, init]) => ((init as RequestInit).method ?? "GET") !== "GET");
    }

    it("givenCategoriaAtiva_whenClicaEmDesativar_thenChamaDeactivate", async () => {
      renderiza();
      await screen.findByText("Documentário");

      await userEvent.click(screen.getByRole("menuitem", { name: "Desativar" }));

      await waitFor(() => expect(chamadasDeEscrita()).toHaveLength(1));
      expect(chamadasDeEscrita()[0]![0]).toBe(`${ADMIN_API}/categories/cat-1/deactivate`);
      expect(await screen.findByText("Categoria desativada.")).toBeInTheDocument();
    });

    it("givenCategoriaInativa_whenClicaEmAtivar_thenChamaActivate", async () => {
      renderiza();
      await screen.findByText("Clássicos");

      await userEvent.click(screen.getByRole("menuitem", { name: "Ativar" }));

      await waitFor(() => expect(chamadasDeEscrita()).toHaveLength(1));
      expect(chamadasDeEscrita()[0]![0]).toBe(`${ADMIN_API}/categories/cat-2/activate`);
      expect(await screen.findByText("Categoria ativada.")).toBeInTheDocument();
    });

    // Remover pergunta antes porque não dá para desfazer. Desativar não pergunta: é reversível no
    // mesmo botão.
    it("givenUmaCategoria_whenClicaEmRemover_thenPerguntaAntesDeChamar", async () => {
      renderiza();
      await screen.findByText("Documentário");

      await userEvent.click(screen.getAllByRole("menuitem", { name: "Remover" })[0]!);

      expect(await screen.findByText("Remover a categoria?")).toBeInTheDocument();
      expect(chamadasDeEscrita()).toHaveLength(0);
    });

    it("givenAPerguntaAberta_whenCancela_thenNaoRemove", async () => {
      renderiza();
      await screen.findByText("Documentário");

      await userEvent.click(screen.getAllByRole("menuitem", { name: "Remover" })[0]!);
      await userEvent.click(await screen.findByRole("button", { name: "Cancelar" }));

      expect(chamadasDeEscrita()).toHaveLength(0);
    });

    it("givenAPerguntaAberta_whenConfirma_thenChamaODelete", async () => {
      renderiza();
      await screen.findByText("Documentário");

      await userEvent.click(screen.getAllByRole("menuitem", { name: "Remover" })[0]!);
      await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

      await waitFor(() => expect(chamadasDeEscrita()).toHaveLength(1));
      expect(chamadasDeEscrita()[0]![0]).toBe(`${ADMIN_API}/categories/cat-1`);
      expect(chamadasDeEscrita()[0]![1]!.method).toBe("DELETE");
      expect(await screen.findByText("Categoria removida.")).toBeInTheDocument();
    });

    // O 409 da admin-api: categoria usada por um gênero ou vídeo **não** se apaga, se desativa.
    it("given409NaRemocao_whenConfirma_thenMandaDesativar", async () => {
      vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) => {
        const metodo = (init as RequestInit | undefined)?.method ?? "GET";
        return Promise.resolve(
          metodo === "GET"
            ? jsonResponse(pagina([DOCUMENTARIO], 1))
            : jsonResponse(
                {
                  message:
                    "Category with ID cat-1 is linked to at least one Genre; deactivate it instead",
                  errors: [],
                },
                { status: 409 },
              ),
        );
      });

      renderiza();
      await screen.findByText("Documentário");

      await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));
      await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

      expect(await screen.findByText(/Desative-o em vez de remover/)).toBeInTheDocument();
    });
  });
});
