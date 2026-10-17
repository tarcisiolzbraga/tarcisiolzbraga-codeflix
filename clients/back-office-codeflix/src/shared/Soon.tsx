import { Alert, Stack, Typography } from "@mui/material";

// Os quatro agregados aparecem nas abas desde já, porque é o mapa do que o back-office administra —
// mas só Categorias tem tela. Uma aba que leva a uma página vazia parece defeito; esta diz o que é.
export function Soon({ agregado }: { agregado: string }) {
  return (
    <Stack spacing={2}>
      <Typography variant="h5">{agregado}</Typography>
      <Alert severity="info">
        Esta tela ainda não existe. A admin-api já expõe as rotas do agregado — falta o CRUD aqui.
      </Alert>
    </Stack>
  );
}
