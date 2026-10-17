import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { newQueryClient } from "@/shared/queryClient";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));

vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CategoryList } = await import("@/categories/CategoryList");

function respondeCom(body: unknown, init: ResponseInit = {}) {
  vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    Promise.resolve(
      body === undefined
        ? new Response(null, init)
        : new Response(JSON.stringify(body), {
            ...init,
            headers: { "Content-Type": "application/json" },
          }),
    ),
  );
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

// O DataGrid precisa de altura medida para desenhar linha, e o jsdom dá zero a tudo. Sem isto a
// tabela existe mas nenhuma célula é renderizada, e o teste acusaria um defeito que não existe.
function comAlturaMedida() {
  for (const propriedade of ["offsetHeight", "offsetWidth"] as const) {
    vi.spyOn(HTMLElement.prototype, propriedade, "get").mockReturnValue(600);
  }
}

function renderiza() {
  return render(
    <QueryClientProvider client={newQueryClient()}>
      <CategoryList />
    </QueryClientProvider>,
  );
}

describe("CategoryList", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    comAlturaMedida();
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
});
