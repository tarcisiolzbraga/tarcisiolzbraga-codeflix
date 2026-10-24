import type { User } from "oidc-client-ts";
import { describe, expect, it } from "vitest";
import { isAdmin, rolesOf } from "@/auth/roles";

// O token é montado como o Keycloak o emite: três partes separadas por ponto, com a carga em
// **base64url**, e os papéis em `realm_access.roles` — **do token de acesso**, não do `profile`.
//
// Antes este dublê devolvia `{ profile: { realm_access: ... } }`, que é uma forma que o Keycloak
// nunca emite: o mapeador de papéis dele nasce com "add to ID token" desligado. Os testes passavam e
// nenhum administrador entrava. O dublê tem de falar a língua do servidor, senão ele prova a si
// mesmo.
function tokenDeAcesso(claims: object): string {
  const base64url = (texto: string) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(texto)))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  return `${base64url(JSON.stringify({ alg: "RS256" }))}.${base64url(JSON.stringify(claims))}.assinatura`;
}

function userComPapeis(roles: unknown): User {
  return { access_token: tokenDeAcesso({ realm_access: { roles } }) } as unknown as User;
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
    const user = { access_token: tokenDeAcesso({ sub: "alguem" }) } as unknown as User;

    expect(rolesOf(user)).toEqual([]);
  });

  // O token vem de fora, e o front não o valida — quem valida é a API. Carga que não é JWT é
  // ausência de papel, e não tela branca.
  it("givenTokenQueNaoEhJwt_whenLidos_thenDevolveListaVazia", () => {
    expect(rolesOf({ access_token: "nada-disso" } as unknown as User)).toEqual([]);
    expect(rolesOf({ access_token: "a.!!!.c" } as unknown as User)).toEqual([]);
  });

  // Acento no nome de quem está logado é caso comum, e `atob` sozinho estraga byte acima de 127.
  it("givenCargaComAcento_whenLida_thenOsPapeisSaemInteiros", () => {
    const user = {
      access_token: tokenDeAcesso({ name: "Operação de Teste", realm_access: { roles: ["CODEFLIX_ADMIN"] } }),
    } as unknown as User;

    expect(rolesOf(user)).toEqual(["CODEFLIX_ADMIN"]);
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
