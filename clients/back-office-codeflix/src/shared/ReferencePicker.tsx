import { Autocomplete, CircularProgress, TextField } from "@mui/material";
import { useState } from "react";
import { REFERENCE_LABELS, type Reference, type ReferenceResource } from "@/shared/references";
import { useReferenceSearch, useReferencesByIds } from "@/shared/useReferences";
import { useDebounced } from "@/shared/useDebounced";

// A escolha de registros de outro agregado: as categorias de um gênero, e as categorias, gêneros e
// membros de elenco de um vídeo. Um campo para os quatro usos, porque os três recursos têm a mesma
// forma no que importa — id e nome.
export function ReferencePicker({
  resource,
  value,
  onChange,
  label,
}: {
  resource: ReferenceResource;
  value: string[];
  onChange: (ids: string[]) => void;
  label?: string;
}) {
  const [digitado, setDigitado] = useState("");
  const busca = useDebounced(digitado);
  const opcoes = useReferenceSearch(resource, busca);
  const escolhidos = useReferencesByIds(resource, value);

  // Os escolhidos entram nas opções mesmo quando a busca não os devolve: sem isso, o Autocomplete
  // acusaria valor fora da lista e a etiqueta perderia o nome logo depois de escolher.
  const disponiveis: Reference[] = [
    ...escolhidos.references,
    ...(opcoes.data?.items ?? []).filter((item) => !value.includes(item.id)),
  ];

  return (
    <Autocomplete<Reference, true>
      multiple
      options={disponiveis}
      value={escolhidos.references}
      loading={opcoes.isFetching || escolhidos.isPending}
      filterOptions={(todas) => todas}
      getOptionLabel={(opcao) => opcao.name}
      isOptionEqualToValue={(opcao, escolhido) => opcao.id === escolhido.id}
      onInputChange={(_, texto) => setDigitado(texto)}
      onChange={(_, escolha) => onChange(escolha.map((opcao) => opcao.id))}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label ?? REFERENCE_LABELS[resource]}
          slotProps={{
            input: {
              ...params.InputProps,
              endAdornment: (
                <>
                  {opcoes.isFetching && <CircularProgress size={16} aria-label="buscando" />}
                  {params.InputProps.endAdornment}
                </>
              ),
            },
          }}
        />
      )}
    />
  );
}
