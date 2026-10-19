import CastIcon from "@mui/icons-material/RecordVoiceOverOutlined";
import CategoryIcon from "@mui/icons-material/SellOutlined";
import GenreIcon from "@mui/icons-material/TheatersOutlined";
import MenuIcon from "@mui/icons-material/Menu";
import VideoIcon from "@mui/icons-material/MovieOutlined";
import {
  AppBar,
  Box,
  Button,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { Link, Outlet, useLocation } from "react-router";
import { useAuth } from "react-oidc-context";
import { ThemeToggle } from "@/shared/ThemeToggle";
import { UploadList } from "@/videos/UploadList";

// As quatro seções são os quatro agregados que o catálogo tem. A da vez sai do caminho atual, e não
// de estado próprio: assim recarregar a página ou abrir um link direto destaca a certa.
const SECOES = [
  { rotulo: "Categorias", caminho: "/categorias", icone: <CategoryIcon /> },
  { rotulo: "Gêneros", caminho: "/generos", icone: <GenreIcon /> },
  { rotulo: "Elenco", caminho: "/elenco", icone: <CastIcon /> },
  { rotulo: "Vídeos", caminho: "/videos", icone: <VideoIcon /> },
];

const LARGURA = 220;

// **Menu lateral, e não barra de abas.**
//
// A barra de abas que havia antes custava: cada tela de agregado é larga — tabela com seis colunas,
// formulário com três vínculos —, e o espaço vertical é o que falta. O lateral some no telefone e
// fica fixo no desktop, que é onde este painel é usado.
function Secoes({ onNavigate }: { onNavigate?: () => void }) {
  const { pathname } = useLocation();

  return (
    <List>
      {SECOES.map((secao) => (
        <ListItemButton
          key={secao.caminho}
          component={Link}
          to={secao.caminho}
          selected={pathname.startsWith(secao.caminho)}
          onClick={onNavigate}
        >
          <ListItemIcon sx={{ minWidth: 40 }}>{secao.icone}</ListItemIcon>
          <ListItemText primary={secao.rotulo} />
        </ListItemButton>
      ))}
    </List>
  );
}

export function Layout() {
  const auth = useAuth();
  const [aberto, setAberto] = useState(false);

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <AppBar
        position="fixed"
        color="default"
        elevation={1}
        // No desktop a barra começa depois do menu, para o título não ficar sobre ele.
        sx={{ width: { md: `calc(100% - ${LARGURA}px)` }, ml: { md: `${LARGURA}px` } }}
      >
        <Toolbar sx={{ gap: 2 }}>
          <IconButton
            aria-label="Abrir o menu"
            edge="start"
            onClick={() => setAberto(true)}
            sx={{ display: { md: "none" } }}
          >
            <MenuIcon />
          </IconButton>

          <Typography variant="body2" sx={{ color: "text.secondary", flexGrow: 1 }}>
            administração do catálogo
          </Typography>

          <ThemeToggle />

          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            {String(auth.user?.profile.name ?? auth.user?.profile.preferred_username ?? "")}
          </Typography>
          <Button size="small" onClick={() => void auth.signoutRedirect()}>
            Sair
          </Button>
        </Toolbar>
      </AppBar>

      {/* Dois Drawer, e não um com `variant` condicional: o temporário precisa de `onClose` e de
          ficar fora do fluxo, o permanente não. Um só, alternando, faria o estado de aberto valer no
          desktop, onde ele não significa nada. */}
      <Drawer
        variant="temporary"
        open={aberto}
        onClose={() => setAberto(false)}
        sx={{ display: { xs: "block", md: "none" } }}
        slotProps={{ paper: { sx: { width: LARGURA } } }}
      >
        <Marca />
        <Secoes onNavigate={() => setAberto(false)} />
      </Drawer>

      <Drawer
        variant="permanent"
        sx={{ display: { xs: "none", md: "block" }, width: LARGURA, flexShrink: 0 }}
        slotProps={{ paper: { sx: { width: LARGURA, boxSizing: "border-box" } } }}
      >
        <Marca />
        <Secoes />
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, sm: 4 }, pt: { xs: 10, sm: 12 } }}>
        <Outlet />
      </Box>

      {/* A fila de envios, visível de qualquer tela: é isso que a torna fila. */}
      <UploadList />
    </Box>
  );
}

function Marca() {
  return (
    <Toolbar>
      <Typography
        component={Link}
        to="/"
        sx={{ fontWeight: 700, color: "error.main", textDecoration: "none" }}
      >
        CODEFLIX
      </Typography>
    </Toolbar>
  );
}
