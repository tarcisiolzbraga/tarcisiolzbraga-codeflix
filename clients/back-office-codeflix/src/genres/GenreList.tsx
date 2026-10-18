import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/EditOutlined";
import ToggleOffIcon from "@mui/icons-material/ToggleOffOutlined";
import ToggleOnIcon from "@mui/icons-material/ToggleOnOutlined";
import { Button, Chip, Stack, Typography } from "@mui/material";
import { GridActionsCellItem, type GridColDef } from "@mui/x-data-grid";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import type { Genre } from "@/genres/genresGateway";
import { useGenres } from "@/genres/useGenres";
import { useDeleteGenre, useSetGenreActive } from "@/genres/useGenreMutations";
import { AggregateTable } from "@/shared/AggregateTable";
import { ConfirmDialog } from "@/shared/ConfirmDialog";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { SEARCH_DEFAULTS, withSearch, type SearchQuery } from "@/shared/search";
import { useDebounced } from "@/shared/useDebounced";

const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export function GenreList() {
  const [digitado, setDigitado] = useState("");
  const [query, setQuery] = useState<SearchQuery>(SEARCH_DEFAULTS);
  const [aRemover, setARemover] = useState<Genre | null>(null);
  const navegar = useNavigate();
  const avisar = useFeedback();

  const consulta = withSearch(query, useDebounced(digitado));
  const generos = useGenres(consulta);
  const alternar = useSetGenreActive();
  const remover = useDeleteGenre();

  const colunas: GridColDef<Genre>[] = [
    { field: "name", headerName: "Nome", flex: 1, minWidth: 200 },
    {
      field: "categories",
      headerName: "Categorias",
      width: 150,
      sortable: false,
      // **A contagem, e não os nomes.** O `GenreListResponse` da admin-api traz só os **ids**, e não
      // há rota que receba vários: mostrar nomes aqui custaria uma chamada por categoria de cada
      // linha. Os nomes aparecem na tela de edição, onde a conta é limitada ao gênero aberto.
      renderCell: ({ value }: { value?: string[] }) => (
        <Chip
          size="small"
          variant="outlined"
          label={
            (value ?? []).length === 1 ? "1 categoria" : `${(value ?? []).length} categorias`
          }
        />
      ),
    },
    {
      field: "active",
      headerName: "Situação",
      width: 120,
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
      width: 130,
      getActions: ({ row }) => [
        <GridActionsCellItem
          key="editar"
          icon={<EditIcon />}
          label="Editar"
          onClick={() => void navegar(`/generos/${row.id}`)}
        />,
        <GridActionsCellItem
          key="alternar"
          icon={row.active ? <ToggleOnIcon /> : <ToggleOffIcon />}
          label={row.active ? "Desativar" : "Ativar"}
          onClick={() =>
            alternar.mutate(
              { id: row.id, active: !row.active },
              {
                onSuccess: () =>
                  avisar({
                    kind: "success",
                    message: row.active ? "Gênero desativado." : "Gênero ativado.",
                  }),
                onError: (erro) => avisar({ kind: "error", message: explainFailure(erro) }),
              },
            )
          }
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
        <Typography variant="h5">Gêneros</Typography>
        <Button component={Link} to="/generos/novo" variant="contained" startIcon={<AddIcon />}>
          Novo gênero
        </Button>
      </Stack>

      <AggregateTable<Genre>
        label="Buscar por nome"
        columns={colunas}
        page={generos.data}
        isFetching={generos.isFetching}
        failure={generos.error}
        query={consulta}
        onQueryChange={setQuery}
        search={digitado}
        onSearchChange={setDigitado}
      />

      <ConfirmDialog
        open={aRemover !== null}
        title="Remover o gênero?"
        message={`"${aRemover?.name ?? ""}" sai do catálogo e não há como desfazer. Gênero usado por algum vídeo não é removido — nesse caso, desative.`}
        confirmLabel="Remover"
        pending={remover.isPending}
        onCancel={() => setARemover(null)}
        onConfirm={() =>
          remover.mutate(aRemover!.id, {
            onSuccess: () => {
              avisar({ kind: "success", message: "Gênero removido." });
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
