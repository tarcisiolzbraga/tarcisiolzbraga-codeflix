import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/EditOutlined";
import ToggleOffIcon from "@mui/icons-material/ToggleOffOutlined";
import ToggleOnIcon from "@mui/icons-material/ToggleOnOutlined";
import { Button, Chip, Stack, Typography } from "@mui/material";
import { GridActionsCellItem, type GridColDef } from "@mui/x-data-grid";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import type { Category } from "@/categories/categoriesGateway";
import { useCategories } from "@/categories/useCategories";
import { useDeleteCategory, useSetCategoryActive } from "@/categories/useCategoryMutations";
import { AggregateTable } from "@/shared/AggregateTable";
import { ConfirmDialog } from "@/shared/ConfirmDialog";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { SEARCH_DEFAULTS, withSearch, type SearchQuery } from "@/shared/search";
import { useDebounced } from "@/shared/useDebounced";

const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export function CategoryList() {
  const [digitado, setDigitado] = useState("");
  const [query, setQuery] = useState<SearchQuery>(SEARCH_DEFAULTS);
  const [aRemover, setARemover] = useState<Category | null>(null);
  const navegar = useNavigate();
  const avisar = useFeedback();

  const consulta = withSearch(query, useDebounced(digitado));
  const categorias = useCategories(consulta);
  const alternar = useSetCategoryActive();
  const remover = useDeleteCategory();

  const colunas: GridColDef<Category>[] = [
    { field: "name", headerName: "Nome", flex: 1, minWidth: 180 },
    { field: "description", headerName: "Descrição", flex: 2, minWidth: 240, sortable: false },
    {
      field: "active",
      headerName: "Situação",
      width: 120,
      renderCell: ({ value }) =>
        value ? (
          <Chip label="ativa" color="success" size="small" variant="outlined" />
        ) : (
          <Chip label="inativa" size="small" variant="outlined" />
        ),
    },
    {
      field: "createdAt",
      headerName: "Criada em",
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
          onClick={() => void navegar(`/categorias/${row.id}`)}
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
                    message: row.active ? "Categoria desativada." : "Categoria ativada.",
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
          // A remoção pergunta antes porque não dá para desfazer. Desativar não pergunta: é
          // reversível no mesmo botão.
          onClick={() => setARemover(row)}
        />,
      ],
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
        <Typography variant="h5">Categorias</Typography>
        <Button component={Link} to="/categorias/nova" variant="contained" startIcon={<AddIcon />}>
          Nova categoria
        </Button>
      </Stack>

      <AggregateTable<Category>
        label="Buscar por nome ou descrição"
        columns={colunas}
        page={categorias.data}
        isFetching={categorias.isFetching}
        failure={categorias.error}
        query={consulta}
        onQueryChange={setQuery}
        search={digitado}
        onSearchChange={setDigitado}
      />

      <ConfirmDialog
        open={aRemover !== null}
        title="Remover a categoria?"
        message={`"${aRemover?.name ?? ""}" sai do catálogo e não há como desfazer. Categoria usada por um gênero ou vídeo não é removida — nesse caso, desative.`}
        confirmLabel="Remover"
        pending={remover.isPending}
        onCancel={() => setARemover(null)}
        onConfirm={() =>
          remover.mutate(aRemover!.id, {
            onSuccess: () => {
              avisar({ kind: "success", message: "Categoria removida." });
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
