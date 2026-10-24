import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";
import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { isAdmin } from "@/auth/roles";
import { PORTAL_URL } from "@/shared/config";

// A porta do back-office, e ela tem **dois** degraus que precisam ser lidos juntos:
//
// 1. estar logado;
// 2. ter o papel de administração.
//
// O segundo não é zelo: o client do Keycloak é o mesmo do portal, então um assinante consegue entrar
// aqui com o token dele. Sem esta conferência, ele veria a interface inteira e tomaria 403 em cada
// chamada — o que parece defeito do sistema, e não falta de permissão.
//
// Conferir no front **não** é a segurança: quem decide é a admin-api, em cada rota. Isto é para a
// pessoa não ficar parada sem entender o que aconteceu.
//
// **Nenhum dos dois degraus mostra tela de espera com botão.** Quem abre `/videos` quer o painel, e
// um botão "Entrar" é um clique a mais sem informação nova; quem não administra o catálogo tem lugar
// melhor para estar, que é o próprio catálogo. As duas situações **levam** a pessoa em vez de
// pararem nela.
export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const auth = useAuth();
  const { isLoading, isAuthenticated, activeNavigator, error, signinRedirect, user } = auth;
  const assinante = isAuthenticated && !isAdmin(user);

  // Sem sessão, vai direto para o Keycloak.
  //
  // As três guardas não são zelo: `activeNavigator` cobre o redirecionamento **já em voo** — sem
  // ela, cada render dispararia outro —, `isLoading` cobre a leitura do `sessionStorage`, e `error`
  // é a que evita o laço: quem volta do Keycloak com falha veria a tela de erro mandá-lo de volta
  // para o Keycloak, para sempre.
  useEffect(() => {
    if (!isLoading && !isAuthenticated && activeNavigator === undefined && error === undefined) {
      void signinRedirect();
    }
  }, [isLoading, isAuthenticated, activeNavigator, error, signinRedirect]);

  // Assinante vai para o catálogo. `replace`, e não `assign`: o histórico não guarda uma tela que
  // ele nunca deveria ter visto, então o "voltar" do browser não o traz de volta para cá — e sem
  // isso o botão de voltar vira um pingue-pongue.
  useEffect(() => {
    if (assinante) {
      window.location.replace(PORTAL_URL);
    }
  }, [assinante]);

  if (error !== undefined) {
    return (
      <Stack spacing={2} sx={{ m: 3, alignItems: "flex-start" }}>
        <Alert severity="error">Falha ao autenticar: {error.message}</Alert>
        {/* O botão existe **porque** a entrada é automática: sem sessão e com erro, não há mais
            nada na tela para a pessoa tentar. */}
        <Button variant="contained" onClick={() => void signinRedirect()}>
          Tentar entrar de novo
        </Button>
      </Stack>
    );
  }

  if (assinante) {
    return (
      <Esperando legenda="Esta é a administração do catálogo. Levando você ao Codeflix…">
        {/* A saída fica à mão para quem quer entrar com outra conta: sem ela, a única forma de
            trocar de conta seria pelo portal. */}
        <Button size="small" onClick={() => void auth.signoutRedirect()}>
          Sair desta conta
        </Button>
      </Esperando>
    );
  }

  if (!isAuthenticated) {
    return <Esperando legenda="Levando você ao login…" />;
  }

  return <>{children}</>;
}

// A mesma espera para os três casos em que a tela está indo para outro lugar. O texto diz **para
// onde**, porque girar sem dizer nada é o que faz parecer travado.
function Esperando({ legenda, children }: { legenda: string; children?: React.ReactNode }) {
  return (
    <Box sx={{ display: "grid", placeItems: "center", minHeight: "60vh" }}>
      <Stack spacing={2} sx={{ alignItems: "center" }}>
        <CircularProgress aria-label="carregando" />
        <Typography sx={{ color: "text.secondary" }}>{legenda}</Typography>
        {children}
      </Stack>
    </Box>
  );
}
