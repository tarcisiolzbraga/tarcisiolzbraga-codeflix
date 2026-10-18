import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CastMemberEdit } = await import("@/castMembers/CastMemberEdit");

const EXISTENTE = {
  id: "cm-1",
  name: "Vin Diesel",
  type: "ACTOR",
  active: false,
  createdAt: "2026-02-03T10:15:00Z",
  updatedAt: "2026-02-04T10:15:00Z",
};

function renderiza() {
  return renderWithProviders(<CastMemberEdit />, {
    path: "/elenco/:id",
    route: "/elenco/cm-1",
  });
}

describe("CastMemberEdit", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) =>
      Promise.resolve(
        ((init as RequestInit | undefined)?.method ?? "GET") === "GET"
          ? jsonResponse(EXISTENTE)
          : jsonResponse({ id: "cm-1" }),
      ),
    );
  });

  it("givenUmMembro_whenAbre_thenCarregaPeloIdDaRota", async () => {
    renderiza();

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(`${ADMIN_API}/cast-members/cm-1`);
    expect(await screen.findByDisplayValue("Vin Diesel")).toBeInTheDocument();
  });

  it("givenUmMembro_whenAbre_thenNaoMostraOInterruptorDeSituacao", async () => {
    renderiza();

    await screen.findByDisplayValue("Vin Diesel");
    expect(screen.queryByLabelText("Nasce ativo")).not.toBeInTheDocument();
  });

  // O corpo da atualização leva nome e papel, e **não** `active`.
  it("givenPapelTrocado_whenSalva_thenMandaPutComNomeEPapel", async () => {
    renderiza();

    await screen.findByDisplayValue("Vin Diesel");
    await userEvent.click(screen.getByRole("combobox", { name: /Papel/ }));
    await userEvent.click(await screen.findByRole("option", { name: "Diretor" }));
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    const put = await waitFor(() => {
      const achada = vi
        .mocked(globalThis.fetch)
        .mock.calls.find(([, init]) => (init as RequestInit).method === "PUT");
      expect(achada).toBeDefined();
      return achada!;
    });

    expect(put[0]).toBe(`${ADMIN_API}/cast-members/cm-1`);
    expect(JSON.parse((put[1] as RequestInit).body as string)).toEqual({
      name: "Vin Diesel",
      type: "DIRECTOR",
    });
    expect(await screen.findByText("Membro de elenco salvo.")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/elenco");
  });

  it("givenMembroInexistente_whenAbre_thenMostraAMensagemDaApi", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        jsonResponse({ message: "CastMember with ID cm-1 was not found" }, { status: 404 }),
      ),
    );

    renderiza();

    expect(await screen.findByText("CastMember with ID cm-1 was not found")).toBeInTheDocument();
  });
});
