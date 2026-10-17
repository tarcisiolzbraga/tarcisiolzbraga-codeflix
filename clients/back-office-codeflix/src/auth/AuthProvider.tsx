import { WebStorageStateStore } from "oidc-client-ts";
import { AuthProvider as OidcProvider } from "react-oidc-context";
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_ISSUER } from "@/shared/config";

// O login do back-office: authorization code + PKCE S256, contra o mesmo client público
// `front-codeflix` do realm que o portal usa.
//
// **O mesmo client para os dois fronts, de propósito.** Ele é "por onde uma PESSOA entra" — quem
// separa o que cada um pode é o **papel** no token, não o client. Um client por front exigiria
// manter duas listas de redirectUris e dois segredos de nada (os dois são públicos), sem ganhar
// controle algum.
//
// A biblioteca é a mesma do portal, e aqui a escolha foi mais fácil: o `@react-keycloak/web` que a
// referência do curso usa teve a última publicação em **abril de 2022**.
const REDIRECT_URI = typeof window === "undefined" ? "" : window.location.origin;

// sessionStorage como no portal: o token morre quando a aba fecha. Num back-office isso vale mais —
// é a diferença entre sair e deixar o catálogo aberto para o próximo que usar a máquina.
const store = () =>
  typeof window === "undefined"
    ? undefined
    : new WebStorageStateStore({ store: window.sessionStorage });

export function AuthProvider({ children }: { children: React.ReactNode }) {
  return (
    <OidcProvider
      authority={KEYCLOAK_ISSUER}
      client_id={KEYCLOAK_CLIENT_ID}
      redirect_uri={REDIRECT_URI}
      post_logout_redirect_uri={REDIRECT_URI}
      response_type="code"
      scope="openid profile email"
      automaticSilentRenew
      userStore={store()}
      // Limpa o `?code=` e o `?state=` da URL depois da volta do Keycloak: sem isto, recarregar a
      // página tenta trocar um código já usado, e o Keycloak recusa — com razão.
      onSigninCallback={() =>
        window.history.replaceState({}, document.title, window.location.pathname)
      }
    >
      {children}
    </OidcProvider>
  );
}
