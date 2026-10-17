import { Alert, Box, Chip, Stack, TextField, Typography } from "@mui/material";
import { DataGrid, type GridColDef, type GridSortModel } from "@mui/x-data-grid";
import { ptBR } from "@mui/x-data-grid/locales";
import { useState } from "react";
import {
  CATEGORY_QUERY_DEFAULTS,
  type CategoryQuery,
  type Category,
} from "@/categories/categoriesGateway";
import { useCategories } from "@/categories/useCategories";
import { explainFailure } from "@/shared/explainFailure";
import { useDebounced } from "@/shared/useDebounced";

const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

const COLUNAS: GridColDef<Category>[] = [
  { field: "name", headerName: "Nome", flex: 1, minWidth: 180 },
  { field: "description", headerName: "Descrição", flex: 2, minWidth: 240, sortable: false },
  {
    field: "active",
    headerName: "Situação",
    width: 130,
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
];

export function CategoryList() {
  const [digitado, setDigitado] = useState("");
  const [query, setQuery] = useState<CategoryQuery>(CATEGORY_QUERY_DEFAULTS);
  const busca = useDebounced(digitado);

  // A busca atrasada entra na consulta junto com a volta para a primeira página: filtrar e continuar
  // na página 4 mostraria "nenhum resultado" num filtro que tem resultados.
  const consulta = busca === query.search ? query : { ...query, search: busca, page: 0 };
  const categorias = useCategories(consulta);

  const ordenacao: GridSortModel = [{ field: consulta.sort, sort: consulta.dir }];

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Categorias</Typography>

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
          columns={COLUNAS}
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
    </Stack>
  );
}
