import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CategoryCreate } = await import("@/categories/CategoryCreate");

function corpoEnviado(): unknown {
  const [, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
  return JSON.parse((init as RequestInit).body as string);
}

describe("CategoryCreate", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ id: "cat-1" }, { status: 201 })),
    );
  });

  it("givenNomeEDescricao_whenCria_thenMandaOPostEVoltaParaALista", async () => {
    renderWithProviders(<CategoryCreate />, { path: "/categorias/nova", route: "/categorias/nova" });

    await userEvent.type(screen.getByLabelText(/Nome/), "Documentário");
    await userEvent.type(screen.getByLabelText("Descrição"), "Histórias reais");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(`${ADMIN_API}/categories`);
    expect(corpoEnviado()).toEqual({
      name: "Documentário",
      description: "Histórias reais",
      active: true,
    });
    expect(await screen.findByText("Categoria criada.")).toBeInTheDocument();
    // `waitFor`, e não asserção direta: o aviso de sucesso e a navegação saem do mesmo
    // `onSuccess`, mas `navegate` do react-router é assíncrono — em máquina lenta os dois
    // renders se separam, e a asserção direta lia a rota antiga. Foi o que o CI pegou.
    await waitFor(() => expect(screen.getByTestId("rota")).toHaveTextContent("/categorias"));
  });

  // Validação do formulário: o nome curto nem sai da tela, porque o limite é conhecido e a mensagem
  // que voltaria da API é em inglês.
  it("givenNomeCurto_whenTentaCriar_thenNaoChamaAApi", async () => {
    renderWithProviders(<CategoryCreate />);

    await userEvent.type(screen.getByLabelText(/Nome/), "do");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("O nome precisa de ao menos 3 caracteres.")).toBeInTheDocument();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenInterruptorDesligado_whenCria_thenMandaActiveFalso", async () => {
    renderWithProviders(<CategoryCreate />);

    await userEvent.type(screen.getByLabelText(/Nome/), "Documentário");
    await userEvent.click(screen.getByLabelText("Nasce ativa"));
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(corpoEnviado()).toMatchObject({ active: false });
  });

  // Descrição vazia vai como `null`, e não como string vazia.
  it("givenDescricaoVazia_whenCria_thenMandaNull", async () => {
    renderWithProviders(<CategoryCreate />);

    await userEvent.type(screen.getByLabelText(/Nome/), "Documentário");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(corpoEnviado()).toMatchObject({ description: null });
  });

  // O 422 com duas mensagens: mostrar só a primeira faria a pessoa corrigir uma coisa e levar o
  // mesmo erro de volta.
  it("given422ComDuasMensagens_whenCria_thenMostraAsDuasENaoNavega", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        jsonResponse(
          {
            message: "'name' should not be empty",
            errors: ["'name' should not be empty", "'description' must be at most 4000 characters"],
          },
          { status: 422 },
        ),
      ),
    );

    renderWithProviders(<CategoryCreate />, { path: "/categorias/nova", route: "/categorias/nova" });

    await userEvent.type(screen.getByLabelText(/Nome/), "Documentário");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("A admin-api recusou por 2 motivos:")).toBeInTheDocument();
    expect(screen.getByText("'name' should not be empty")).toBeInTheDocument();
    expect(screen.getByText("'description' must be at most 4000 characters")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/categorias/nova");
  });
});
