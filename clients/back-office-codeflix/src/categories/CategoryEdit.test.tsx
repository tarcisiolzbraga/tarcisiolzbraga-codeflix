import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CategoryEdit } = await import("@/categories/CategoryEdit");

const EXISTENTE = {
  id: "cat-1",
  name: "Documentário",
  description: "Histórias reais",
  active: false,
  createdAt: "2026-02-03T10:15:00Z",
  updatedAt: "2026-02-04T10:15:00Z",
};

function renderiza() {
  return renderWithProviders(<CategoryEdit />, {
    path: "/categorias/:id",
    route: "/categorias/cat-1",
  });
}

describe("CategoryEdit", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    vi.spyOn(globalThis, "fetch").mockImplementation((url) =>
      Promise.resolve(
        String(url).endsWith("/categories/cat-1")
          ? jsonResponse(EXISTENTE)
          : jsonResponse({ id: "cat-1" }),
      ),
    );
  });

  it("givenUmaCategoria_whenAbre_thenCarregaPeloIdDaRota", async () => {
    renderiza();

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(`${ADMIN_API}/categories/cat-1`);
    expect(await screen.findByDisplayValue("Documentário")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Histórias reais")).toBeInTheDocument();
  });

  // A tela de edição busca pelo id em vez de reaproveitar a linha da tabela: a linha pode estar
  // velha, e salvar por cima do que outra pessoa mudou é pior que uma ida à rede.
  it("givenUmaCategoria_whenAbre_thenNaoMostraOInterruptorDeSituacao", async () => {
    renderiza();

    await screen.findByDisplayValue("Documentário");
    expect(screen.queryByLabelText("Nasce ativa")).not.toBeInTheDocument();
  });

  // O corpo da atualização **não** leva `active`: a rota da admin-api não mexe nisso, e ativar e
  // desativar são ações da lista.
  it("givenNomeTrocado_whenSalva_thenMandaPutSoComNomeEDescricao", async () => {
    renderiza();

    const nome = await screen.findByDisplayValue("Documentário");
    await userEvent.clear(nome);
    await userEvent.type(nome, "Documentários");
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    // A chamada é procurada pelo verbo, e não por índice: depois do PUT, o `invalidateQueries`
    // refaz o GET do detalhe antes da navegação terminar, então a ordem tem três chamadas.
    const put = await waitFor(() => {
      const achada = vi
        .mocked(globalThis.fetch)
        .mock.calls.find(([, init]) => (init as RequestInit).method === "PUT");
      expect(achada).toBeDefined();
      return achada!;
    });

    expect(put[0]).toBe(`${ADMIN_API}/categories/cat-1`);
    expect(JSON.parse((put[1] as RequestInit).body as string)).toEqual({
      name: "Documentários",
      description: "Histórias reais",
    });
    expect(await screen.findByText("Categoria salva.")).toBeInTheDocument();
    // `waitFor`, e não asserção direta: o aviso de sucesso e a navegação saem do mesmo
    // `onSuccess`, mas `navegate` do react-router é assíncrono — em máquina lenta os dois
    // renders se separam, e a asserção direta lia a rota antiga. Foi o que o CI pegou.
    await waitFor(() => expect(screen.getByTestId("rota")).toHaveTextContent("/categorias"));
  });

  it("givenCategoriaInexistente_whenAbre_thenMostraAMensagemDaApi", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        jsonResponse({ message: "Category with ID cat-1 was not found" }, { status: 404 }),
      ),
    );

    renderiza();

    expect(await screen.findByText("Category with ID cat-1 was not found")).toBeInTheDocument();
  });
});
