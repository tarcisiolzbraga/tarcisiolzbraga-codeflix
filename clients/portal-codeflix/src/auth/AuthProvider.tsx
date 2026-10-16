"use client";

import { WebStorageStateStore } from "oidc-client-ts";
import { AuthProvider as OidcProvider } from "react-oidc-context";
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_ISSUER } from "@/shared/config";

// O login do portal: authorization code + PKCE S256, contra o client público `front-codeflix` do
// realm. É cliente (`"use client"`) porque o fluxo inteiro vive no browser — a troca de código por
// token usa o `code_verifier` que só existe ali.
//
// `oidc-client-ts` em vez do `keycloak-js`: o adaptador oficial do Keycloak está no 26.2.4 e o
// servidor do compose no 26.8.0, ou seja, a biblioteca depende do ritmo de release do servidor. Este
// é padrão OIDC puro, tem binding de React de verdade (`react-oidc-context`) e dá controle explícito
// da renovação — que aqui não é detalhe: **o papel do catálogo só entra no token depois de renovar**,
// porque a assinatura o concede por evento, segundos depois.
//
// E não `next-auth`: o v5, que é o de App Router, está em beta há muito tempo, e o v4 estável é da
// API antiga. Guardar token no browser com biblioteca estável é melhor que sessão de servidor em
// beta — e combina com o CORS que as três APIs já aceitam.
const REDIRECT_URI = typeof window === "undefined" ? "" : window.location.origin;

// sessionStorage, não localStorage: o token morre quando a aba fecha. Em máquina compartilhada isso
// é a diferença entre sair e continuar logado para o próximo que abrir o browser.
const store = () =>
  typeof window === "undefined" ? undefined : new WebStorageStateStore({ store: window.sessionStorage });

export function AuthProvider({ children }: { children: React.ReactNode }) {
  return (
    <OidcProvider
      authority={KEYCLOAK_ISSUER}
      client_id={KEYCLOAK_CLIENT_ID}
      redirect_uri={REDIRECT_URI}
      post_logout_redirect_uri={REDIRECT_URI}
      response_type="code"
      scope="openid profile email"
      // Renova sozinho antes de expirar: sem isto, uma aba aberta por mais que o tempo de vida do
      // token passa a receber 401 no meio da navegação.
      automaticSilentRenew
      userStore={store()}
      // Limpa o `?code=` e o `?state=` da URL depois da volta do Keycloak. Sem isto, recarregar a
      // página tenta trocar um código já usado, e o Keycloak recusa — com razão.
      onSigninCallback={() => window.history.replaceState({}, document.title, window.location.pathname)}
    >
      {children}
    </OidcProvider>
  );
}
