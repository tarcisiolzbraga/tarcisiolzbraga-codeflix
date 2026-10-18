import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/EditOutlined";
import ToggleOffIcon from "@mui/icons-material/ToggleOffOutlined";
import ToggleOnIcon from "@mui/icons-material/ToggleOnOutlined";
import { Button, Chip, Stack, Typography } from "@mui/material";
import { GridActionsCellItem, type GridColDef } from "@mui/x-data-grid";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import {
  CAST_MEMBER_TYPE_LABELS,
  type CastMember,
  type CastMemberType,
} from "@/castMembers/castMembersGateway";
import { useCastMembers } from "@/castMembers/useCastMembers";
import {
  useDeleteCastMember,
  useSetCastMemberActive,
} from "@/castMembers/useCastMemberMutations";
import { AggregateTable } from "@/shared/AggregateTable";
import { ConfirmDialog } from "@/shared/ConfirmDialog";
import { explainFailure } from "@/shared/explainFailure";
import { useFeedback } from "@/shared/feedback";
import { SEARCH_DEFAULTS, withSearch, type SearchQuery } from "@/shared/search";
import { useDebounced } from "@/shared/useDebounced";

const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export function CastMemberList() {
  const [digitado, setDigitado] = useState("");
  const [query, setQuery] = useState<SearchQuery>(SEARCH_DEFAULTS);
  const [aRemover, setARemover] = useState<CastMember | null>(null);
  const navegar = useNavigate();
  const avisar = useFeedback();

  const consulta = withSearch(query, useDebounced(digitado));
  const membros = useCastMembers(consulta);
  const alternar = useSetCastMemberActive();
  const remover = useDeleteCastMember();

  const colunas: GridColDef<CastMember>[] = [
    { field: "name", headerName: "Nome", flex: 1, minWidth: 200 },
    {
      field: "type",
      headerName: "Papel",
      width: 140,
      // O rótulo é em português, mas o valor ordenado é o que vai para a API: `sort=type` ordena
      // por `ACTOR` e `DIRECTOR`, que é o que está na coluna.
      valueFormatter: (value: CastMemberType) => CAST_MEMBER_TYPE_LABELS[value],
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
          onClick={() => void navegar(`/elenco/${row.id}`)}
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
                    message: row.active ? "Membro desativado." : "Membro ativado.",
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
        <Typography variant="h5">Elenco</Typography>
        <Button component={Link} to="/elenco/novo" variant="contained" startIcon={<AddIcon />}>
          Novo membro
        </Button>
      </Stack>

      <AggregateTable<CastMember>
        label="Buscar por nome"
        columns={colunas}
        page={membros.data}
        isFetching={membros.isFetching}
        failure={membros.error}
        query={consulta}
        onQueryChange={setQuery}
        search={digitado}
        onSearchChange={setDigitado}
      />

      <ConfirmDialog
        open={aRemover !== null}
        title="Remover o membro de elenco?"
        message={`"${aRemover?.name ?? ""}" sai do catálogo e não há como desfazer. Membro usado por algum vídeo não é removido — nesse caso, desative.`}
        confirmLabel="Remover"
        pending={remover.isPending}
        onCancel={() => setARemover(null)}
        onConfirm={() =>
          remover.mutate(aRemover!.id, {
            onSuccess: () => {
              avisar({ kind: "success", message: "Membro de elenco removido." });
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
