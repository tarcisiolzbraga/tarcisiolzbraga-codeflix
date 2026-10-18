import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders, withMeasuredHeight } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { CastMemberList } = await import("@/castMembers/CastMemberList");

const ATOR = {
  id: "cm-1",
  name: "Vin Diesel",
  type: "ACTOR",
  active: true,
  createdAt: "2026-02-03T10:15:00Z",
};

const DIRETOR = {
  id: "cm-2",
  name: "Denis Villeneuve",
  type: "DIRECTOR",
  active: false,
  createdAt: "2026-02-04T10:15:00Z",
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
  return renderWithProviders(<CastMemberList />, { path: "/elenco", route: "/elenco" });
}

describe("CastMemberList", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    withMeasuredHeight();
    respondeComLista([]);
  });

  it("givenConsultaPadrao_whenAbre_thenPedeAPrimeiraPaginaDosCastMembers", async () => {
    renderiza();

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled());
    expect(vi.mocked(globalThis.fetch).mock.calls[0]![0]).toBe(
      `${ADMIN_API}/cast-members?page=0&perPage=10&sort=name&dir=asc`,
    );
  });

  // O papel vai para a API em maiúsculas e aparece na tela em português.
  it("givenUmAtorEUmDiretor_whenAbre_thenMostraOsPapeisEmPortugues", async () => {
    respondeComLista([ATOR, DIRETOR]);

    renderiza();

    expect(await screen.findByText("Vin Diesel")).toBeInTheDocument();
    expect(screen.getByText("Ator")).toBeInTheDocument();
    expect(screen.getByText("Denis Villeneuve")).toBeInTheDocument();
    expect(screen.getByText("Diretor")).toBeInTheDocument();
    expect(screen.queryByText("ACTOR")).not.toBeInTheDocument();
  });

  it("givenUmMembroAtivoEUmInativo_whenAbre_thenMostraASituacaoNoMasculino", async () => {
    respondeComLista([ATOR, DIRETOR]);

    renderiza();

    expect(await screen.findByText("ativo")).toBeInTheDocument();
    expect(screen.getByText("inativo")).toBeInTheDocument();
  });

  it("givenALista_whenAbre_thenOfereceCriarUmNovo", async () => {
    renderiza();

    expect(await screen.findByRole("link", { name: /Novo membro/ })).toHaveAttribute(
      "href",
      "/elenco/novo",
    );
  });

  it("givenMembroAtivo_whenClicaEmDesativar_thenChamaDeactivate", async () => {
    respondeComLista([ATOR]);

    renderiza();
    await screen.findByText("Vin Diesel");
    await userEvent.click(screen.getByRole("menuitem", { name: "Desativar" }));

    await waitFor(() => expect(escritas()).toHaveLength(1));
    expect(escritas()[0]![0]).toBe(`${ADMIN_API}/cast-members/cm-1/deactivate`);
    expect(await screen.findByText("Membro desativado.")).toBeInTheDocument();
  });

  it("givenMembroInativo_whenClicaEmAtivar_thenChamaActivate", async () => {
    respondeComLista([DIRETOR]);

    renderiza();
    await screen.findByText("Denis Villeneuve");
    await userEvent.click(screen.getByRole("menuitem", { name: "Ativar" }));

    await waitFor(() => expect(escritas()).toHaveLength(1));
    expect(escritas()[0]![0]).toBe(`${ADMIN_API}/cast-members/cm-2/activate`);
  });

  it("givenUmMembro_whenClicaEmRemover_thenPerguntaAntesDeChamar", async () => {
    respondeComLista([ATOR]);

    renderiza();
    await screen.findByText("Vin Diesel");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));

    expect(await screen.findByText("Remover o membro de elenco?")).toBeInTheDocument();
    expect(escritas()).toHaveLength(0);
  });

  it("givenAPerguntaAberta_whenConfirma_thenChamaODelete", async () => {
    respondeComLista([ATOR]);

    renderiza();
    await screen.findByText("Vin Diesel");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));
    await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

    await waitFor(() => expect(escritas()).toHaveLength(1));
    expect(escritas()[0]![1]!.method).toBe("DELETE");
    expect(await screen.findByText("Membro de elenco removido.")).toBeInTheDocument();
  });

  // Membro usado por algum vídeo **não** se apaga, se desativa — a admin-api responde 409.
  it("given409NaRemocao_whenConfirma_thenMandaDesativar", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) => {
      const metodo = (init as RequestInit | undefined)?.method ?? "GET";
      return Promise.resolve(
        metodo === "GET"
          ? jsonResponse(pagina([ATOR]))
          : jsonResponse(
              {
                message:
                  "CastMember with ID cm-1 is linked to at least one Video; deactivate it instead",
                errors: [],
              },
              { status: 409 },
            ),
      );
    });

    renderiza();
    await screen.findByText("Vin Diesel");
    await userEvent.click(screen.getByRole("menuitem", { name: "Remover" }));
    await userEvent.click(await screen.findByRole("button", { name: "Remover" }));

    expect(await screen.findByText(/Desative-o em vez de remover/)).toBeInTheDocument();
  });
});
