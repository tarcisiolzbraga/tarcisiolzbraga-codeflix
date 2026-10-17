import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/EditOutlined";
import ToggleOffIcon from "@mui/icons-material/ToggleOffOutlined";
import ToggleOnIcon from "@mui/icons-material/ToggleOnOutlined";
import { Alert, Box, Button, Chip, Stack, TextField, Typography } from "@mui/material";
import { DataGrid, GridActionsCellItem, type GridColDef, type GridSortModel } from "@mui/x-data-grid";
import { ptBR } from "@mui/x-data-grid/locales";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import {
  CATEGORY_QUERY_DEFAULTS,
  type CategoryQuery,
  type Category,
} from "@/categories/categoriesGateway";
import { useCategories } from "@/categories/useCategories";
import { useDeleteCategory, useSetCategoryActive } from "@/categories/useCategoryMutations";
import { ConfirmDialog } from "@/shared/ConfirmDialog";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { useDebounced } from "@/shared/useDebounced";

const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export function CategoryList() {
  const [digitado, setDigitado] = useState("");
  const [query, setQuery] = useState<CategoryQuery>(CATEGORY_QUERY_DEFAULTS);
  const [aRemover, setARemover] = useState<Category | null>(null);
  const busca = useDebounced(digitado);
  const navegar = useNavigate();
  const avisar = useFeedback();

  // A busca atrasada entra na consulta junto com a volta para a primeira página: filtrar e continuar
  // na página 4 mostraria "nenhum resultado" num filtro que tem resultados.
  const consulta = busca === query.search ? query : { ...query, search: busca, page: 0 };
  const categorias = useCategories(consulta);
  const alternar = useSetCategoryActive();
  const remover = useDeleteCategory();

  const ordenacao: GridSortModel = [{ field: consulta.sort, sort: consulta.dir }];

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

      <TextField
        label="Buscar por nome ou descrição"
        value={digitado}
        onChange={(evento) => setDigitado(evento.target.value)}
        size="small"
        sx={{ maxWidth: 420 }}
      />

      {categorias.isError && <Alert severity="error">{explainFailure(categorias.error)}</Alert>}

      <Box sx={{ height: 520 }}>
        <DataGrid<Category>
          rows={categorias.data?.items ?? []}
          columns={colunas}
          localeText={ptBR.components.MuiDataGrid.defaultProps.localeText}
          loading={categorias.isFetching}
          disableColumnFilter
          disableRowSelectionOnClick
          // Paginação e ordenação no servidor: o banco do catálogo não cabe no navegador, e é a
          // mesma razão pela qual nenhuma rota da admin-api devolve List.
          paginationMode="server"
          sortingMode="server"
          rowCount={categorias.data?.total ?? 0}
          pageSizeOptions={[10, 25, 50]}
          paginationModel={{ page: consulta.page, pageSize: consulta.perPage }}
          onPaginationModelChange={(modelo) =>
            setQuery({ ...consulta, page: modelo.page, perPage: modelo.pageSize })
          }
          sortModel={ordenacao}
          onSortModelChange={([primeiro]) =>
            setQuery({
              ...consulta,
              sort: primeiro?.field ?? CATEGORY_QUERY_DEFAULTS.sort,
              dir: primeiro?.sort ?? CATEGORY_QUERY_DEFAULTS.dir,
            })
          }
        />
      </Box>

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
