import { Box, CircularProgress, CssBaseline, ThemeProvider } from "@mui/material";
import { Suspense, lazy } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router";
import { AuthProvider } from "@/auth/AuthProvider";
import { RequireAdmin } from "@/auth/RequireAdmin";
import { FeedbackProvider } from "@/shared/FeedbackProvider";
import { Layout } from "@/shared/Layout";
import { QueryProvider } from "@/shared/QueryProvider";
import { theme } from "@/shared/theme";

// As telas entram por `lazy` porque o DataGrid é o pedaço mais pesado da aplicação, e ninguém
// precisa dele para ver a tela de entrar. Sem isto o navegador baixa a tabela inteira antes de
// desenhar o botão de login.
const CategoryList = lazy(() =>
  import("@/categories/CategoryList").then((m) => ({ default: m.CategoryList })),
);
const CategoryCreate = lazy(() =>
  import("@/categories/CategoryCreate").then((m) => ({ default: m.CategoryCreate })),
);
const CategoryEdit = lazy(() =>
  import("@/categories/CategoryEdit").then((m) => ({ default: m.CategoryEdit })),
);
const CastMemberList = lazy(() =>
  import("@/castMembers/CastMemberList").then((m) => ({ default: m.CastMemberList })),
);
const CastMemberCreate = lazy(() =>
  import("@/castMembers/CastMemberCreate").then((m) => ({ default: m.CastMemberCreate })),
);
const CastMemberEdit = lazy(() =>
  import("@/castMembers/CastMemberEdit").then((m) => ({ default: m.CastMemberEdit })),
);
const GenreList = lazy(() => import("@/genres/GenreList").then((m) => ({ default: m.GenreList })));
const GenreCreate = lazy(() =>
  import("@/genres/GenreCreate").then((m) => ({ default: m.GenreCreate })),
);
const GenreEdit = lazy(() => import("@/genres/GenreEdit").then((m) => ({ default: m.GenreEdit })));
const VideoList = lazy(() => import("@/videos/VideoList").then((m) => ({ default: m.VideoList })));
const VideoCreate = lazy(() =>
  import("@/videos/VideoCreate").then((m) => ({ default: m.VideoCreate })),
);
const VideoEdit = lazy(() => import("@/videos/VideoEdit").then((m) => ({ default: m.VideoEdit })));

function Carregando() {
  return (
    <Box sx={{ display: "grid", placeItems: "center", minHeight: "40vh" }}>
      <CircularProgress aria-label="carregando a tela" />
    </Box>
  );
}

// A ordem dos provedores não é gosto: o `RequireAdmin` precisa do AuthProvider acima dele, e as
// telas precisam do QueryProvider. O guarda fica **fora** do Layout de propósito — quem não tem o
// papel não deve ver nem as abas do catálogo.
export function App() {
  return (
    <ThemeProvider theme={theme} defaultMode="dark">
      <CssBaseline />
      <AuthProvider>
        <QueryProvider>
          <FeedbackProvider>
            <BrowserRouter>
              <RequireAdmin>
                <Suspense fallback={<Carregando />}>
                  <Routes>
                    <Route element={<Layout />}>
                      <Route index element={<Navigate to="/categorias" replace />} />
                      <Route path="categorias" element={<CategoryList />} />
                      {/* `nova` antes de `:id`, senão a rota com parâmetro casaria com ela e a
                          tela de criação tentaria carregar uma categoria de id "nova". */}
                      <Route path="categorias/nova" element={<CategoryCreate />} />
                      <Route path="categorias/:id" element={<CategoryEdit />} />
                      <Route path="generos" element={<GenreList />} />
                      <Route path="generos/novo" element={<GenreCreate />} />
                      <Route path="generos/:id" element={<GenreEdit />} />
                      <Route path="elenco" element={<CastMemberList />} />
                      <Route path="elenco/novo" element={<CastMemberCreate />} />
                      <Route path="elenco/:id" element={<CastMemberEdit />} />
                      <Route path="videos" element={<VideoList />} />
                      <Route path="videos/novo" element={<VideoCreate />} />
                      <Route path="videos/:id" element={<VideoEdit />} />
                    </Route>
                  </Routes>
                </Suspense>
              </RequireAdmin>
            </BrowserRouter>
          </FeedbackProvider>
        </QueryProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
