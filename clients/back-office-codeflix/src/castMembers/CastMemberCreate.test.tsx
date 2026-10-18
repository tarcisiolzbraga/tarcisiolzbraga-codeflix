import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CastMemberCreate } = await import("@/castMembers/CastMemberCreate");

function corpoEnviado(): unknown {
  const [, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
  return JSON.parse((init as RequestInit).body as string);
}

describe("CastMemberCreate", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ id: "cm-1" }, { status: 201 })),
    );
  });

  // Ator é o padrão porque é o papel mais comum num catálogo, e porque o campo é obrigatório — sem
  // padrão, criar um ator exigiria uma escolha que não acrescenta nada.
  it("givenSoONome_whenCria_thenMandaAtorAtivo", async () => {
    renderWithProviders(<CastMemberCreate />, { path: "/elenco/novo", route: "/elenco/novo" });

    await userEvent.type(screen.getByLabelText(/Nome/), "Vin Diesel");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(`${ADMIN_API}/cast-members`);
    expect(corpoEnviado()).toEqual({ name: "Vin Diesel", type: "ACTOR", active: true });
    expect(await screen.findByText("Membro de elenco criado.")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/elenco");
  });

  // O papel é escolha fechada: a API aceita `diretor` em minúsculas, mas texto livre deixaria a
  // pessoa digitar "atriz" e tomar um 422 em inglês por algo que a tela sabia de antemão.
  it("givenPapelDeDiretor_whenEscolhido_thenVaiEmMaiusculas", async () => {
    renderWithProviders(<CastMemberCreate />);

    await userEvent.type(screen.getByLabelText(/Nome/), "Denis Villeneuve");
    await userEvent.click(screen.getByRole("combobox", { name: /Papel/ }));
    await userEvent.click(await screen.findByRole("option", { name: "Diretor" }));
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(corpoEnviado()).toMatchObject({ type: "DIRECTOR" });
  });

  it("givenNomeCurto_whenTentaCriar_thenNaoChamaAApi", async () => {
    renderWithProviders(<CastMemberCreate />);

    await userEvent.type(screen.getByLabelText(/Nome/), "Vi");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("O nome precisa de ao menos 3 caracteres.")).toBeInTheDocument();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenInterruptorDesligado_whenCria_thenMandaActiveFalso", async () => {
    renderWithProviders(<CastMemberCreate />);

    await userEvent.type(screen.getByLabelText(/Nome/), "Vin Diesel");
    await userEvent.click(screen.getByLabelText("Nasce ativo"));
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(corpoEnviado()).toMatchObject({ active: false });
  });

  it("given422_whenCria_thenMostraAMensagemENaoNavega", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        jsonResponse(
          { message: "'type' should be one of [ACTOR, DIRECTOR]", errors: [] },
          { status: 422 },
        ),
      ),
    );

    renderWithProviders(<CastMemberCreate />, { path: "/elenco/novo", route: "/elenco/novo" });

    await userEvent.type(screen.getByLabelText(/Nome/), "Vin Diesel");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("'type' should be one of [ACTOR, DIRECTOR]")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/elenco/novo");
  });
});
