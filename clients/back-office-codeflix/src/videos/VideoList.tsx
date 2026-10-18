import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/EditOutlined";
import { Button, Chip, Stack, Typography } from "@mui/material";
import { GridActionsCellItem, type GridColDef } from "@mui/x-data-grid";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { AggregateTable } from "@/shared/AggregateTable";
import { ConfirmDialog } from "@/shared/ConfirmDialog";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { ReferencePicker } from "@/shared/ReferencePicker";
import { withSearch, type SearchQuery } from "@/shared/search";
import { useDebounced } from "@/shared/useDebounced";
import {
  VIDEO_FILTERS_EMPTY,
  VIDEO_SEARCH_DEFAULTS,
  type VideoFilters,
  type VideoRow,
} from "@/videos/videosGateway";
import { useDeleteVideo } from "@/videos/useVideoMutations";
import { useVideos } from "@/videos/useVideos";

const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export function VideoList() {
  const [digitado, setDigitado] = useState("");
  // A ordenação padrão é por **`title`**, e não por `name`: é o padrão do `VideoSearchRequest`, e o
  // único dos quatro agregados que difere.
  const [query, setQuery] = useState<SearchQuery>(VIDEO_SEARCH_DEFAULTS);
  const [filtros, setFiltros] = useState<VideoFilters>(VIDEO_FILTERS_EMPTY);
  const [aRemover, setARemover] = useState<VideoRow | null>(null);
  const navegar = useNavigate();
  const avisar = useFeedback();

  const consulta = withSearch(query, useDebounced(digitado));
  const videos = useVideos(consulta, filtros);
  const remover = useDeleteVideo();

  const colunas: GridColDef<VideoRow>[] = [
    { field: "title", headerName: "Título", flex: 1, minWidth: 220 },
    { field: "launchedAt", headerName: "Ano", width: 90 },
    {
      field: "published",
      headerName: "Publicado",
      width: 120,
      renderCell: ({ value }) =>
        value ? (
          <Chip label="publicado" color="success" size="small" variant="outlined" />
        ) : (
          <Chip label="rascunho" size="small" variant="outlined" />
        ),
    },
    {
      field: "active",
      headerName: "Situação",
      width: 110,
      renderCell: ({ value }) =>
        value ? (
          <Chip label="ativo" color="success" size="small" variant="outlined" />
        ) : (
          <Chip label="inativo" size="small" variant="outlined" />
        ),
    },
    {
      field: "createdAt",
      headerName: "Criado em",
      width: 170,
      valueFormatter: (value: string) => dataHora.format(new Date(value)),
    },
    {
      field: "acoes",
      type: "actions",
      headerName: "Ações",
      width: 100,
      // **Sem os interruptores de estado aqui.** Ativo, publicado e aberto são três decisões
      // diferentes, e `opened` libera o arquivo para quem não paga: elas ficam na tela de edição,
      // onde cada uma vem com o que significa escrito ao lado.
      getActions: ({ row }) => [
        <GridActionsCellItem
          key="editar"
          icon={<EditIcon />}
          label="Editar"
          onClick={() => void navegar(`/videos/${row.id}`)}
        />,
        <GridActionsCellItem
          key="remover"
          icon={<DeleteIcon />}
          label="Remover"
          onClick={() => setARemover(row)}
        />,
      ],
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
        <Typography variant="h5">Vídeos</Typography>
        <Button component={Link} to="/videos/novo" variant="contained" startIcon={<AddIcon />}>
          Novo vídeo
        </Button>
      </Stack>

      {/* Os filtros por vínculo são da rota de vídeo, e de nenhuma outra: o `VideoSearchRequest`
          recebe os três conjuntos de ids. */}
      <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
        <ReferencePicker
          resource="categories"
          label="Filtrar por categorias"
          value={filtros.categories}
          onChange={(categories) => setFiltros({ ...filtros, categories })}
        />
        <ReferencePicker
          resource="genres"
          label="Filtrar por gêneros"
          value={filtros.genres}
          onChange={(genres) => setFiltros({ ...filtros, genres })}
        />
        <ReferencePicker
          resource="cast-members"
          label="Filtrar por elenco"
          value={filtros.castMembers}
          onChange={(castMembers) => setFiltros({ ...filtros, castMembers })}
        />
      </Stack>

      <AggregateTable<VideoRow>
        label="Buscar por título"
        columns={colunas}
        page={videos.data}
        isFetching={videos.isFetching}
        failure={videos.error}
        query={consulta}
        onQueryChange={setQuery}
        search={digitado}
        onSearchChange={setDigitado}
      />

      <ConfirmDialog
        open={aRemover !== null}
        title="Remover o vídeo?"
        message={`"${aRemover?.title ?? ""}" sai do catálogo com os arquivos enviados, e não há como desfazer. Para tirar do ar sem apagar, despublique ou desative na tela de edição.`}
        confirmLabel="Remover"
        pending={remover.isPending}
        onCancel={() => setARemover(null)}
        onConfirm={() =>
          remover.mutate(aRemover!.id, {
            onSuccess: () => {
              avisar({ kind: "success", message: "Vídeo removido." });
              setARemover(null);
            },
            onError: (erro) => {
              avisar({ kind: "error", message: explainFailure(erro) });
              setARemover(null);
            },
          })
        }
      />
    </Stack>
  );
}
