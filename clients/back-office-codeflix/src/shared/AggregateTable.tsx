import { Alert, Box, Stack, TextField } from "@mui/material";
import { DataGrid, type GridColDef, type GridSortModel } from "@mui/x-data-grid";
import { ptBR } from "@mui/x-data-grid/locales";
import { SEARCH_DEFAULTS, type SearchQuery } from "@/shared/search";
import { explainFailure } from "@/shared/explainFailure";
import type { Page } from "@/shared/http";

// A tabela que os quatro agregados dividem: campo de busca, aviso de falha, e paginação e ordenação
// **no servidor**. O que muda entre eles são as colunas, e elas vêm por prop.
//
// Isto existe porque era a terceira cópia. As duas primeiras ainda cabiam em cada tela; a terceira
// já seria uma decisão mantida em três lugares — a de não trazer `List` do banco, que é a mesma
// razão pela qual nenhuma rota de listagem da admin-api devolve lista inteira.
export function AggregateTable<T extends { id: string }>({
  label,
  columns,
  page,
  isFetching,
  failure,
  query,
  onQueryChange,
  search,
  onSearchChange,
}: {
  label: string;
  columns: GridColDef<T>[];
  page: Page<T> | undefined;
  isFetching: boolean;
  failure: unknown;
  query: SearchQuery;
  onQueryChange: (query: SearchQuery) => void;
  search: string;
  onSearchChange: (search: string) => void;
}) {
  const ordenacao: GridSortModel = [{ field: query.sort, sort: query.dir }];

  return (
    <Stack spacing={3}>
      <TextField
        label={label}
        value={search}
        onChange={(evento) => onSearchChange(evento.target.value)}
        size="small"
        sx={{ maxWidth: 420 }}
      />

      {failure !== null && failure !== undefined && (
        <Alert severity="error">{explainFailure(failure)}</Alert>
      )}

      <Box sx={{ height: 520 }}>
        <DataGrid<T>
          rows={page?.items ?? []}
          columns={columns}
          localeText={ptBR.components.MuiDataGrid.defaultProps.localeText}
          loading={isFetching}
          disableColumnFilter
          disableRowSelectionOnClick
          paginationMode="server"
          sortingMode="server"
          rowCount={page?.total ?? 0}
          pageSizeOptions={[10, 25, 50]}
          paginationModel={{ page: query.page, pageSize: query.perPage }}
          onPaginationModelChange={(modelo) =>
            onQueryChange({ ...query, page: modelo.page, perPage: modelo.pageSize })
          }
          sortModel={ordenacao}
          onSortModelChange={([primeiro]) =>
            onQueryChange({
              ...query,
              sort: primeiro?.field ?? SEARCH_DEFAULTS.sort,
              dir: primeiro?.sort ?? SEARCH_DEFAULTS.dir,
            })
          }
        />
      </Box>
    </Stack>
  );
}
