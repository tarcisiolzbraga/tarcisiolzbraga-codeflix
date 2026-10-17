import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";
import { useAuth } from "react-oidc-context";
import { isAdmin } from "@/auth/roles";

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
// pessoa entender o que está acontecendo.
export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const auth = useAuth();

  if (auth.isLoading) {
    return (
      <Box sx={{ display: "grid", placeItems: "center", minHeight: "60vh" }}>
        <CircularProgress aria-label="carregando" />
      </Box>
    );
  }

  if (auth.error) {
    return (
      <Alert severity="error" sx={{ m: 3 }}>
        Falha ao autenticar: {auth.error.message}
      </Alert>
    );
  }

  if (!auth.isAuthenticated) {
    return (
      <Stack spacing={2} sx={{ m: 3, alignItems: "flex-start" }}>
        <Typography>Entre para administrar o catálogo.</Typography>
        <Button variant="contained" onClick={() => void auth.signinRedirect()}>
          Entrar
        </Button>
      </Stack>
    );
  }

  if (!isAdmin(auth.user)) {
    return (
      <Stack spacing={2} sx={{ m: 3, alignItems: "flex-start" }}>
        <Alert severity="warning">
          Sua conta não tem permissão de administração do catálogo. Se você acabou de receber o
          acesso, saia e entre de novo — o papel entra no token na próxima sessão.
        </Alert>
        <Button onClick={() => void auth.signoutRedirect()}>Sair</Button>
      </Stack>
    );
  }

  return <>{children}</>;
}
