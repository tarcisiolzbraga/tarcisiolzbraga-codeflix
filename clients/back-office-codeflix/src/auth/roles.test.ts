import type { User } from "oidc-client-ts";
import { describe, expect, it } from "vitest";
import { isAdmin, rolesOf } from "@/auth/roles";

// O token é montado à mão porque é o formato que importa: `realm_access.roles`, como o Keycloak o
// escreve. Se o caminho mudar, estes testes caem — que é exatamente o aviso que se quer.
function userComPapeis(roles: unknown): User {
  return { profile: { realm_access: { roles } } } as unknown as User;
}

describe("rolesOf", () => {
  it("givenTokenComPapeis_whenLidos_thenDevolveALista", () => {
    const user = userComPapeis(["CODEFLIX_ADMIN", "CODEFLIX_CATEGORIES"]);

    expect(rolesOf(user)).toEqual(["CODEFLIX_ADMIN", "CODEFLIX_CATEGORIES"]);
  });

  it("givenNinguemLogado_whenLidos_thenDevolveListaVazia", () => {
    expect(rolesOf(null)).toEqual([]);
    expect(rolesOf(undefined)).toEqual([]);
  });

  it("givenTokenSemRealmAccess_whenLidos_thenDevolveListaVazia", () => {
    const user = { profile: {} } as unknown as User;

    expect(rolesOf(user)).toEqual([]);
  });

  // Defensivo de propósito: o claim vem de fora, e um `roles` que não é lista derrubaria a tela
  // inteira com "includes is not a function" em vez de dizer "sem permissão".
  it("givenRolesQueNaoEhLista_whenLidos_thenDevolveListaVazia", () => {
    expect(rolesOf(userComPapeis("CODEFLIX_ADMIN"))).toEqual([]);
  });

  it("givenPapelQueNaoEhTexto_whenLidos_thenEleEhDescartado", () => {
    expect(rolesOf(userComPapeis(["CODEFLIX_ADMIN", 7, null]))).toEqual(["CODEFLIX_ADMIN"]);
  });
});

describe("isAdmin", () => {
  it("givenContaComPapelDeAdministracao_whenConferida_thenEhAdmin", () => {
    expect(isAdmin(userComPapeis(["CODEFLIX_ADMIN"]))).toBe(true);
  });

  // O caso que o guarda existe para pegar: um assinante entra com o mesmo client do Keycloak.
  it("givenAssinante_whenConferido_thenNaoEhAdmin", () => {
    expect(isAdmin(userComPapeis(["CODEFLIX_ACCOUNT", "CODEFLIX_SUBSCRIBER"]))).toBe(false);
  });

  it("givenPapelDeUmAgregadoSo_whenConferido_thenNaoEhAdmin", () => {
    expect(isAdmin(userComPapeis(["CODEFLIX_CATEGORIES"]))).toBe(false);
  });
});
