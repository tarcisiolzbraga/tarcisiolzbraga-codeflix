import { Box, CircularProgress } from "@mui/material";
import { Suspense, lazy } from "react";
import { Navigate, Route, Routes } from "react-router";
import { Layout } from "@/shared/Layout";
import { NotFound } from "@/shared/NotFound";

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

// A tabela de rotas mora fora do `App` para poder ser medida: afirmar que um endereço inexistente
// cai na tela de "não existe" exige a tabela **de verdade**. O teste que montava a sua própria rota
// `*` provava que o componente desenha, não que o roteador o alcança — e continuava verde com a
// rota `*` apagada do App.
export function AppRoutes() {
  return (
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
          {/* Endereço que não existe cai aqui, e não numa tela vazia — que num painel
              de administração parece defeito do sistema. */}
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
