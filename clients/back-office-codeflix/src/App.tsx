import { CssBaseline, ThemeProvider } from "@mui/material";
import { BrowserRouter } from "react-router";
import { AppRoutes } from "@/AppRoutes";
import { AuthProvider } from "@/auth/AuthProvider";
import { RequireAdmin } from "@/auth/RequireAdmin";
import { FeedbackProvider } from "@/shared/FeedbackProvider";
import { UploadQueueProvider } from "@/videos/UploadQueueProvider";
import { QueryProvider } from "@/shared/QueryProvider";
import { theme } from "@/shared/theme";

// A ordem dos provedores não é gosto: o `RequireAdmin` precisa do AuthProvider acima dele, e as
// telas precisam do QueryProvider. O guarda fica **fora** do Layout de propósito — quem não tem o
// papel não deve ver nem o menu do catálogo.
export function App() {
  return (
    <ThemeProvider theme={theme} defaultMode="dark">
      <CssBaseline />
      <AuthProvider>
        <QueryProvider>
          <FeedbackProvider>
            {/* A fila de envios fica **acima do roteador**: é isso que a faz sobreviver à
                navegação, e é o que a torna fila em vez de um botão com estado. Dentro do
                QueryProvider porque ela invalida o vídeo quando um arquivo chega. */}
            <UploadQueueProvider>
              <BrowserRouter>
                <RequireAdmin>
                  <AppRoutes />
                </RequireAdmin>
              </BrowserRouter>
            </UploadQueueProvider>
          </FeedbackProvider>
        </QueryProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
