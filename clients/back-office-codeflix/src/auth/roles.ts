import type { User } from "oidc-client-ts";
import { ADMIN_ROLE } from "@/shared/config";

// Os papéis do realm vêm do **token de acesso**, e não do `profile`.
//
// O `profile` do `oidc-client-ts` são as claims do **id token**, e o mapeador de papéis do Keycloak
// nasce com "add to ID token" desligado: `realm_access` existe só no token de acesso. Ler do
// profile respondia "sua conta não tem permissão" para quem **tem** o papel — e nenhum teste pegava,
// porque o dublê montava um `profile` que o Keycloak nunca emite.
//
// Ler do token de acesso tem uma segunda razão, melhor que a primeira: é **esse** token que vai nas
// chamadas à API. Decidir por outra fonte deixaria o front mostrar o painel para quem vai tomar 403
// em cada rota — justamente o que este guarda existe para evitar.
//
// Os papéis vêm **sem** o prefixo `ROLE_` que o Spring acrescenta do outro lado.
function claimsOf(token: string | undefined): { realm_access?: { roles?: unknown } } | undefined {
  const carga = token?.split(".")[1];
  if (carga === undefined) {
    return undefined;
  }
  try {
    // O Keycloak assina em base64**url** — `-` e `_` no lugar de `+` e `/` —, e o `atob` só aceita
    // base64 comum, com o preenchimento completo.
    const base64 = carga.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(carga.length / 4) * 4, "=");
    const bytes = Uint8Array.from(atob(base64), (caractere) => caractere.charCodeAt(0));
    // `TextDecoder`, e não `atob` direto: nome de papel é ASCII, mas o resto da carga não é — e
    // `atob` sozinho estraga byte acima de 127.
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    // Token que não é JWT não é motivo para derrubar a tela: é ausência de papel.
    return undefined;
  }
}

export function rolesOf(user: User | null | undefined): string[] {
  const roles = claimsOf(user?.access_token)?.realm_access?.roles;
  return Array.isArray(roles) ? roles.filter((role): role is string => typeof role === "string") : [];
}

export function isAdmin(user: User | null | undefined): boolean {
  return rolesOf(user).includes(ADMIN_ROLE);
}
