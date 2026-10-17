import type { User } from "oidc-client-ts";
import { ADMIN_ROLE } from "@/shared/config";

// Os papéis do realm, como o Keycloak os põe no token: dentro de `realm_access.roles`, **sem** o
// prefixo `ROLE_` que o Spring acrescenta do outro lado.
//
// Fica numa função própria porque é a mesma leitura que o KeycloakJwtConverter faz nos serviços —
// e porque errar o caminho aqui daria "sem permissão" para quem tem.
export function rolesOf(user: User | null | undefined): string[] {
  const claim = user?.profile as { realm_access?: { roles?: unknown } } | undefined;
  const roles = claim?.realm_access?.roles;
  return Array.isArray(roles) ? roles.filter((role): role is string => typeof role === "string") : [];
}

export function isAdmin(user: User | null | undefined): boolean {
  return rolesOf(user).includes(ADMIN_ROLE);
}
