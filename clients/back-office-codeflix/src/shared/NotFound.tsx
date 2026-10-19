import { Alert, Button, Stack, Typography } from "@mui/material";
import { Link, useLocation } from "react-router";

// A rota que não existe.
//
// Sem ela, uma URL errada caía numa tela **vazia** — e tela vazia num painel de administração parece
// defeito do sistema, não endereço errado. Isto diz qual endereço falhou, porque link quebrado em
// e-mail ou em anotação é a causa comum.
export function NotFound() {
  const { pathname } = useLocation();

  return (
    <Stack spacing={3} sx={{ alignItems: "flex-start" }}>
      <Typography variant="h5">Esta tela não existe</Typography>
      <Alert severity="info">
        Nada responde em <code>{pathname}</code>. Se você chegou por um link, ele pode estar velho.
      </Alert>
      <Button component={Link} to="/categorias" variant="contained">
        Ir para as categorias
      </Button>
    </Stack>
  );
}
