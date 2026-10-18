import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders, withMeasuredHeight } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { GenreList } = await import("@/genres/GenreList");

const ACAO = {
  id: "gen-1",
  name: "Ação",
  categories: ["cat-1", "cat-2", "cat-3"],
  active: true,
  createdAt: "2026-02-03T10:15:00Z",
};

const UMA_SO = {
  id: "gen-2",
  name: "Drama",
  categories: ["cat-1"],
  active: false,
  createdAt: "2026-02-04T10:15:00Z",
};

const SEM_CATEGORIA = {
  id: "gen-3",
  name: "Suspense",
  categories: [],
  active: true,
  createdAt: "2026-02-05T10:15:00Z",
};

function pagina(items: unknown[]) {
  return { currentPage: 0, perPage: 10, total: items.length, items };
}

function respondeComLista(items: unknown[]) {
  vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) => {
    const metodo = (init as RequestInit | undefined)?.method ?? "GET";
    return Promise.resolve(
      metodo === "GET" ? jsonResponse(pagina(items)) : jsonResponse(undefined, { status: 204 }),
    );
  });
}

function escritas() {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.filter(([, init]) => ((init as RequestInit).method ?? "GET") !== "GET");
}

function renderiza() {
  return renderWithProviders(<GenreList />, { path: "/generos", route: "/generos" });
}

describe("GenreList", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    withMeasuredHeight();
    respondeComLista([]);
  });

  it("givenConsultaPadrao_whenAbre_thenPedeAPrimeiraPaginaDosGenres", async () => {
    renderiza();

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(
      `${ADMIN_API}/genres?page=0&perPage=10&sort=name&dir=asc`,
    );
  });

  // A coluna mostra a **contagem**, e não os nomes: o `GenreListResponse` traz só ids, e não há rota
  // que receba vários — nomes aqui custariam uma chamada por categoria de cada linha.
  it("givenGeneroComTresCategorias_whenAbre_thenMostraAContagem", async () => {
    respondeComLista([ACAO]);

    renderiza();

    expect(await screen.findByText("3 categorias")).toBeInTheDocument();
  });

  it("givenGeneroComUmaCategoria_whenAbre_thenODizNoSingular", async () => {
    respondeComLista([UMA_SO]);

    renderiza();

    expect(await screen.findByText("1 categoria")).toBeInTheDocument();
  });

  // Gênero sem categoria é estado válido no domínio, e a coluna precisa dizer isso em vez de ficar
  // vazia.
  it("givenGeneroSemCategoria_whenAbre_thenMostraZero", async () => {
    respondeComLista([SEM_CATEGORIA]);

    renderiza();

    expect(await screen.findByText("0 categorias")).toBeInTheDocument();
  });

  it("givenALista_whenAbre_thenNaoMostraIdDeCategoria", async () => {
    respondeComLista([ACAO]);

    renderiza();

    await screen.findByText("Ação");
    expect(screen.queryByText(/cat-1/)).not.toBeInTheDocument();
  });

  it("givenALista_whenAbre_thenOfereceCriarUmNovo", async () => {
    renderiza();

    expect(await screen.findByRole("link", { name: /Novo gênero/ })).toHaveAttribute(
      "href",
      "/generos/novo",
    );
  });

  it("givenGeneroAtivo_whenClicaEmDesativar_thenChamaDeactivate", async () => {
    respondeComLista([ACAO]);

    renderiza();
    await screen.findByText("Ação");
    await userEvent.click(screen.getByRole("menuitem", { name: "Desativar" }));

    await waitFor(() => expect(escritas()).toHaveLength(1));
    expect(escritas()[0]![0]).toBe(`${ADMIN_API}/genres/gen-1/deactivate`);
    expect(await screen.findByText("Gênero desativado.")).toBeInTheDocument();
  });

  it("givenUmGenero_whenClicaEmRemover_thenPerguntaAntesDeChamar", async () => {
    respondeComLista([ACAO]);

    renderiza();
    await screen.findByText("Ação");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));

    expect(await screen.findByText("Remover o gênero?")).toBeInTheDocument();
    expect(escritas()).toHaveLength(0);
  });

  it("givenAPerguntaAberta_whenConfirma_thenChamaODelete", async () => {
    respondeComLista([ACAO]);

    renderiza();
    await screen.findByText("Ação");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));
    await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

    await waitFor(() => expect(escritas()).toHaveLength(1));
    expect(escritas()[0]![1]!.method).toBe("DELETE");
    expect(await screen.findByText("Gênero removido.")).toBeInTheDocument();
  });

  // Gênero usado por algum vídeo **não** se apaga, se desativa.
  it("given409NaRemocao_whenConfirma_thenMandaDesativar", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) => {
      const metodo = (init as RequestInit | undefined)?.method ?? "GET";
      return Promise.resolve(
        metodo === "GET"
          ? jsonResponse(pagina([ACAO]))
          : jsonResponse(
              {
                message: "Genre with ID gen-1 is linked to at least one Video; deactivate it instead",
                errors: [],
              },
              { status: 409 },
            ),
      );
    });

    renderiza();
    await screen.findByText("Ação");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));
    await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

    expect(await screen.findByText(/Desative-o em vez de remover/)).toBeInTheDocument();
  });
});
