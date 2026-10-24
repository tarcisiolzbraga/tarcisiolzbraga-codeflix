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

// O token de acesso como o Keycloak o emite — é de lá que os papéis são lidos, e não do `profile`.
function usuarioCom(roles: string[]): User {
  const base64url = (texto: string) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(texto)))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  const carga = base64url(JSON.stringify({ realm_access: { roles } }));
  return { access_token: `${base64url('{"alg":"RS256"}')}.${carga}.assinatura` } as unknown as User;
}

const DENTRO = "painel do catálogo";

// O `location` é dublado porque o guarda **troca de página** para levar o assinante ao catálogo, e
// no jsdom isso estouraria "not implemented: navigation". O `vi.stubGlobal` é desfeito pelo
// `restoreAllMocks` do setup, então não vaza para o teste seguinte.
function espiaoDeNavegacao() {
  const replace = vi.fn();
  vi.stubGlobal("location", { ...window.location, replace, assign: vi.fn() });
  return replace;
}

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
    sessao.atual = {
      isLoading: false,
      error: new Error("emissor fora do ar"),
      signinRedirect: vi.fn(),
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(screen.getByText(/emissor fora do ar/)).toBeInTheDocument();
  });

  // **Sem tela intermediária.** Quem abre o painel sem sessão vai para o Keycloak sozinho: o botão
  // "Entrar" que havia aqui era um clique a mais sem informação nova.
  it("givenNinguemLogado_whenRenderiza_thenVaiSozinhoParaOKeycloak", () => {
    const signinRedirect = vi.fn(() => Promise.resolve());
    sessao.atual = { isLoading: false, isAuthenticated: false, user: null, signinRedirect };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(signinRedirect).toHaveBeenCalledOnce();
    expect(screen.queryByRole("button", { name: "Entrar" })).not.toBeInTheDocument();
    expect(screen.queryByText(DENTRO)).not.toBeInTheDocument();
  });

  // A guarda que impede o laço: voltar do Keycloak com falha mostraria a tela de erro, que
  // mandaria de volta para o Keycloak, para sempre.
  it("givenFalhaDeAutenticacao_whenRenderiza_thenNaoRedirecionaDeNovo", () => {
    const signinRedirect = vi.fn(() => Promise.resolve());
    sessao.atual = {
      isLoading: false,
      isAuthenticated: false,
      user: null,
      error: new Error("emissor fora do ar"),
      signinRedirect,
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(signinRedirect).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Tentar entrar de novo" })).toBeInTheDocument();
  });

  // A outra guarda: com um redirecionamento em voo, cada render pediria outro.
  it("givenRedirecionamentoEmVoo_whenRenderiza_thenNaoPedeOutro", () => {
    const signinRedirect = vi.fn(() => Promise.resolve());
    sessao.atual = {
      isLoading: false,
      isAuthenticated: false,
      user: null,
      activeNavigator: "signinRedirect",
      signinRedirect,
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(signinRedirect).not.toHaveBeenCalled();
  });

  // O caso que este guarda existe para pegar: o client do Keycloak é o mesmo do portal, então um
  // assinante **consegue** se autenticar aqui. Sem o segundo degrau ele veria a interface inteira e
  // tomaria 403 em cada chamada, o que parece defeito do sistema.
  it("givenAssinanteLogado_whenRenderiza_thenEhLevadoAoCatalogo", () => {
    const replace = espiaoDeNavegacao();
    sessao.atual = {
      isLoading: false,
      isAuthenticated: true,
      user: usuarioCom(["CODEFLIX_ACCOUNT"]),
      signoutRedirect: vi.fn(),
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(replace).toHaveBeenCalledWith("http://localhost:3000");
    expect(screen.queryByText(DENTRO)).not.toBeInTheDocument();
  });

  // `replace`, e não `assign`: o histórico não guarda uma tela que o assinante nunca deveria ter
  // visto, senão o "voltar" do browser vira um pingue-pongue entre os dois fronts.
  it("givenAssinanteLogado_whenLevado_thenOHistoricoNaoGuardaOPainel", () => {
    const replace = espiaoDeNavegacao();
    const assign = window.location.assign;
    sessao.atual = {
      isLoading: false,
      isAuthenticated: true,
      user: usuarioCom(["CODEFLIX_SUBSCRIBER"]),
      signoutRedirect: vi.fn(),
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);

    expect(replace).toHaveBeenCalledOnce();
    expect(assign).not.toHaveBeenCalled();
  });

  // A saída fica à mão: sem ela, trocar de conta só seria possível pelo portal.
  it("givenAssinanteLogado_whenQuerTrocarDeConta_thenPodeSair", async () => {
    espiaoDeNavegacao();
    const signoutRedirect = vi.fn(() => Promise.resolve());
    sessao.atual = {
      isLoading: false,
      isAuthenticated: true,
      user: usuarioCom(["CODEFLIX_ACCOUNT"]),
      signoutRedirect,
    };

    render(<RequireAdmin>{DENTRO}</RequireAdmin>);
    screen.getByRole("button", { name: "Sair desta conta" }).click();

    expect(signoutRedirect).toHaveBeenCalledOnce();
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
