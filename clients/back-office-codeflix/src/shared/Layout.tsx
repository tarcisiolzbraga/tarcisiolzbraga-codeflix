import { AppBar, Box, Button, Container, Tab, Tabs, Toolbar, Typography } from "@mui/material";
import { Link, Outlet, useLocation } from "react-router";
import { useAuth } from "react-oidc-context";

// As quatro abas são os quatro agregados que o catálogo tem. A da vez sai do caminho atual, e não de
// estado próprio: assim recarregar a página ou abrir um link direto mostra a aba certa.
const SECOES = [
  { rotulo: "Categorias", caminho: "/categorias" },
  { rotulo: "Gêneros", caminho: "/generos" },
  { rotulo: "Elenco", caminho: "/elenco" },
  { rotulo: "Vídeos", caminho: "/videos" },
];

export function Layout() {
  const auth = useAuth();
  const { pathname } = useLocation();
  const atual = SECOES.find((secao) => pathname.startsWith(secao.caminho))?.caminho ?? false;

  return (
    <Box sx={{ display: "flex", flexDirection: "column", minHeight: "100vh" }}>
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography component={Link} to="/" sx={{ fontWeight: 700, color: "error.main", textDecoration: "none" }}>
            CODEFLIX
          </Typography>
          <Typography variant="body2" sx={{ color: "text.secondary", flexGrow: 1 }}>
            administração do catálogo
          </Typography>
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            {String(auth.user?.profile.name ?? auth.user?.profile.preferred_username ?? "")}
          </Typography>
          <Button size="small" onClick={() => void auth.signoutRedirect()}>
            Sair
          </Button>
        </Toolbar>
        <Tabs value={atual} sx={{ px: 2 }}>
          {SECOES.map((secao) => (
            <Tab key={secao.caminho} label={secao.rotulo} value={secao.caminho} component={Link} to={secao.caminho} />
          ))}
        </Tabs>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4, flexGrow: 1 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
