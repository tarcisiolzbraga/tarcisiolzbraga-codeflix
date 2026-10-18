import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { GenreEdit } = await import("@/genres/GenreEdit");

const DOCUMENTARIO = { id: "cat-1", name: "Documentário" };

const EXISTENTE = {
  id: "gen-1",
  name: "Ação",
  categories: ["cat-1"],
  active: false,
  createdAt: "2026-02-03T10:15:00Z",
  updatedAt: "2026-02-04T10:15:00Z",
};

function respondeCom(genero: unknown = EXISTENTE, status = 200) {
  vi.spyOn(globalThis, "fetch").mockImplementation((url, init) => {
    const texto = String(url);
    if (((init as RequestInit | undefined)?.method ?? "GET") === "PUT") {
      return Promise.resolve(jsonResponse({ id: "gen-1" }));
    }
    if (texto.includes("/genres/")) {
      return Promise.resolve(jsonResponse(genero, { status }));
    }
    if (/\/categories\/[^?]+$/.test(texto)) {
      return Promise.resolve(jsonResponse(DOCUMENTARIO));
    }
    return Promise.resolve(
      jsonResponse({ currentPage: 0, perPage: 20, total: 1, items: [DOCUMENTARIO] }),
    );
  });
}

function renderiza() {
  return renderWithProviders(<GenreEdit />, { path: "/generos/:id", route: "/generos/gen-1" });
}

async function putEnviado() {
  return await waitFor(() => {
    const achada = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([, init]) => (init as RequestInit).method === "PUT");
    expect(achada).toBeDefined();
    return achada!;
  });
}

describe("GenreEdit", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    respondeCom();
  });

  it("givenUmGenero_whenAbre_thenCarregaPeloIdDaRota", async () => {
    renderiza();

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(`${ADMIN_API}/genres/gen-1`);
    expect(await screen.findByDisplayValue("Ação")).toBeInTheDocument();
  });

  // Os ids que a API devolve viram nomes na tela: é a conta limitada ao gênero aberto.
  it("givenCategoriasPorId_whenAbre_thenMostraOsNomes", async () => {
    renderiza();

    expect(await screen.findByText("Documentário")).toBeInTheDocument();
  });

  it("givenUmGenero_whenAbre_thenNaoMostraOInterruptorDeSituacao", async () => {
    renderiza();

    await screen.findByDisplayValue("Ação");
    expect(screen.queryByLabelText("Nasce ativo")).not.toBeInTheDocument();
  });

  it("givenNomeTrocado_whenSalva_thenMandaPutSemActive", async () => {
    renderiza();

    const nome = await screen.findByDisplayValue("Ação");
    await userEvent.clear(nome);
    await userEvent.type(nome, "Aventura");
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    const put = await putEnviado();
    expect(put[0]).toBe(`${ADMIN_API}/genres/gen-1`);
    expect(JSON.parse((put[1] as RequestInit).body as string)).toEqual({
      name: "Aventura",
      categories: ["cat-1"],
    });
    expect(await screen.findByText("Gênero salvo.")).toBeInTheDocument();
    // `waitFor`, e não asserção direta: o aviso de sucesso e a navegação saem do mesmo
    // `onSuccess`, mas `navegate` do react-router é assíncrono — em máquina lenta os dois
    // renders se separam, e a asserção direta lia a rota antiga. Foi o que o CI pegou.
    await waitFor(() => expect(screen.getByTestId("rota")).toHaveTextContent("/generos"));
  });

  // `categories` **substitui** a lista inteira: tirar a última manda lista vazia, que é como a API
  // desvincula. Mandar o campo ausente deixaria a categoria lá.
  it("givenACategoriaRetirada_whenSalva_thenMandaListaVazia", async () => {
    renderiza();

    await screen.findByText("Documentário");
    // Backspace no campo retira a última etiqueta, que é o que o Autocomplete do Material faz — e é
    // uma ação de gente, diferente de procurar o ícone de fechar no DOM. O `Escape` fecha a lista
    // que o clique abriu; sem ele, o nome continuaria na tela como **opção**, e a conferência de
    // ausência seria ambígua.
    await userEvent.click(screen.getByLabelText("Categorias"));
    await userEvent.keyboard("{Escape}{Backspace}");
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    const put = await putEnviado();
    expect(JSON.parse((put[1] as RequestInit).body as string)).toEqual({
      name: "Ação",
      categories: [],
    });
  });

  it("givenGeneroInexistente_whenAbre_thenMostraAMensagemDaApi", async () => {
    respondeCom({ message: "Genre with ID gen-1 was not found" }, 404);

    renderiza();

    expect(await screen.findByText("Genre with ID gen-1 was not found")).toBeInTheDocument();
  });
});
