import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { GenreCreate } = await import("@/genres/GenreCreate");

const TERROR = { id: "cat-2", name: "Terror" };

function respondeCom(criacao: { body: unknown; status: number } = { body: { id: "gen-1" }, status: 201 }) {
  vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    if (((init as RequestInit | undefined)?.method ?? "GET") === "POST") {
      return Promise.resolve(jsonResponse(criacao.body, { status: criacao.status }));
    }
    const porId = /\/categories\/([^?]+)$/.exec(String(url));
    if (porId !== null) {
      return Promise.resolve(jsonResponse(TERROR));
    }
    return Promise.resolve(
      jsonResponse({ currentPage: 0, perPage: 20, total: 1, items: [TERROR] }),
    );
  });
}

function corpoDoPost(): unknown {
  const [, init] = vi
    .mocked(globalThis.fetch)
    .mock.calls.find(([, i]) => (i as RequestInit).method === "POST")!;
  return JSON.parse((init as RequestInit).body as string);
}

describe("GenreCreate", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    respondeCom();
  });

  // Gênero sem categoria é estado válido no domínio, então criar só com o nome tem de funcionar.
  it("givenSoONome_whenCria_thenMandaListaVaziaDeCategorias", async () => {
    renderWithProviders(<GenreCreate />, { path: "/generos/novo", route: "/generos/novo" });

    await userEvent.type(screen.getByLabelText(/^Nome/), "Ação");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(corpoDoPost()).toBeDefined());
    expect(corpoDoPost()).toEqual({ name: "Ação", categories: [], active: true });
    expect(await screen.findByText("Gênero criado.")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/generos");
  });

  it("givenUmaCategoriaEscolhida_whenCria_thenMandaOIdDela", async () => {
    renderWithProviders(<GenreCreate />);

    await userEvent.type(screen.getByLabelText(/^Nome/), "Ação");
    await userEvent.click(screen.getByLabelText("Categorias"));
    await userEvent.click(await screen.findByRole("option", { name: "Terror" }));
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(corpoDoPost()).toBeDefined());
    expect(corpoDoPost()).toMatchObject({ categories: ["cat-2"] });
  });

  it("givenNomeEmBranco_whenTentaCriar_thenNaoChamaOPost", async () => {
    renderWithProviders(<GenreCreate />);

    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("O nome é obrigatório.")).toBeInTheDocument();
    expect(
      vi.mocked(globalThis.fetch).mock.calls.filter(([, i]) => (i as RequestInit).method === "POST"),
    ).toHaveLength(0);
  });

  // A admin-api confere se as categorias existem, e nomeia as que faltam. A mensagem é dela.
  it("given422DeCategoriaInexistente_whenCria_thenMostraAMensagemDaApi", async () => {
    respondeCom({
      body: {
        message: "Some categories could not be found: cat-9",
        errors: ["Some categories could not be found: cat-9"],
      },
      status: 422,
    });

    renderWithProviders(<GenreCreate />, { path: "/generos/novo", route: "/generos/novo" });

    await userEvent.type(screen.getByLabelText(/^Nome/), "Ação");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("Some categories could not be found: cat-9")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/generos/novo");
  });
});
