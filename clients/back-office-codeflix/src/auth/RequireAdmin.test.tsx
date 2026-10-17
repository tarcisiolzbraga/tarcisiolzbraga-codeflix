import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { User } from "oidc-client-ts";

// A sessão é trocada por teste. O `vi.hoisted` existe porque a fábrica do `vi.mock` sobe para o topo
// do arquivo, antes de qualquer `let` comum.
const sessao = vi.hoisted(() => ({
  atual: {} as Record<string, unknown>,
}));

vi.mock("react-oidc-context", () => ({
  useAuth: () => sessao.atual,
}));

const { RequireAdmin } = await import("@/auth/RequireAdmin");

function usuarioCom(roles: string[]): User {
  return { profile: { realm_access: { roles } } } as unknown as User;
}

const DENTRO = "painel do catálogo";

describe("RequireAdmin", () => {
  beforeEach(() => {
    sessao.atual = { isLoading: false, isAuthenticated: false, user: null };
  });

  it("givenSessaoCarregando_whenRenderiza_thenMostraEspera", () => {
    sessao.atual = { isLoading: true, isAuthenticated: false, user: null };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(screen.getByLabelText("carregando")).toBeInTheDocument();
    expect(screen.queryByText(DENTRO)).not.toBeInTheDocument();
  });

  it("givenFalhaDeAutenticacao_whenRenderiza_thenMostraOMotivo", () => {
    sessao.atual = { isLoading: false, error: new Error("emissor fora do ar") };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(screen.getByText(/emissor fora do ar/)).toBeInTheDocument();
  });

  it("givenNinguemLogado_whenRenderiza_thenOfereceEntrar", () => {
    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(screen.getByRole("button", { name: "Entrar" })).toBeInTheDocument();
    expect(screen.queryByText(DENTRO)).not.toBeInTheDocument();
  });

  it("givenNinguemLogado_whenClicaEmEntrar_thenMandaParaOKeycloak", async () => {
    const signinRedirect = vi.fn(() => Promise.resolve());
    sessao.atual = { isLoading: false, isAuthenticated: false, user: null, signinRedirect };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);
    screen.getByRole("button", { name: "Entrar" }).click();

    expect(signinRedirect).toHaveBeenCalledOnce();
  });

  // O caso que este guarda existe para pegar: o client do Keycloak é o mesmo do portal, então um
  // assinante **consegue** se autenticar aqui. Sem o segundo degrau ele veria a interface inteira e
  // tomaria 403 em cada chamada, o que parece defeito do sistema.
  it("givenAssinanteLogado_whenRenderiza_thenNaoDeixaEntrarEExplica", () => {
    sessao.atual = {
      isLoading: false,
      isAuthenticated: true,
      user: usuarioCom(["CODEFLIX_ACCOUNT"]),
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(screen.queryByText(DENTRO)).not.toBeInTheDocument();
    expect(screen.getByText(/não tem permissão de administração/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sair" })).toBeInTheDocument();
  });

  it("givenAdministradorLogado_whenRenderiza_thenMostraOPainel", () => {
    sessao.atual = {
      isLoading: false,
      isAuthenticated: true,
      user: usuarioCom(["CODEFLIX_ADMIN"]),
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(screen.getByText(DENTRO)).toBeInTheDocument();
  });
});
