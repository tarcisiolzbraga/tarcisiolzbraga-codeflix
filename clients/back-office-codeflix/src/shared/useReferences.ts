import { keepPreviousData, useQueries, useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import {
  getReference,
  searchReferences,
  REFERENCE_QUERY_KEYS,
  type Reference,
  type ReferenceResource,
} from "@/shared/references";
import { SEARCH_DEFAULTS } from "@/shared/search";

// A busca de opções é **no servidor**, como a da tabela e pelo mesmo motivo: o catálogo não cabe num
// `<select>`. Vinte por busca basta para escolher digitando, e não é promessa de completude — quem
// não vê o que quer digita mais.
const POR_BUSCA = 20;

// O elenco e o gênero ordenam por `name`; o vídeo, por `title`. Aqui os três recursos escolhíveis
// ordenam por `name`, que é o que eles têm.
export function useReferenceSearch(resource: ReferenceResource, search: string) {
  const token = useAuth().user?.access_token;

  return useQuery({
    queryKey: [resource, { ...SEARCH_DEFAULTS, search, perPage: POR_BUSCA }],
    queryFn: () =>
      searchReferences(resource, { ...SEARCH_DEFAULTS, search, perPage: POR_BUSCA }, token!),
    enabled: token !== undefined,
    placeholderData: keepPreviousData,
  });
}

// Resolve ids em nomes, uma consulta por id.
//
// **Por que não uma listagem:** a admin-api não tem rota que receba vários ids, e pedir
// `perPage=9999` para montar um mapa seria trazer o catálogo inteiro para o navegador — a coisa que
// nenhuma rota de listagem do monorepo faz. Aqui a conta é limitada ao que o registro aberto tem.
export function useReferencesByIds(resource: ReferenceResource, ids: string[]) {
  const token = useAuth().user?.access_token;

  return useQueries({
    queries: ids.map((id) => ({
      queryKey: [REFERENCE_QUERY_KEYS[resource], id],
      queryFn: () => getReference(resource, id, token!),
      enabled: token !== undefined,
      // Um registro removido responde 404, e repetir não o traz de volta.
      retry: false,
    })),
    combine: (resultados) => ({
      // O id continua aparecendo quando o nome não pôde ser lido — registro removido fora daqui, ou
      // falha de rede. Esconder a etiqueta faria o vínculo parecer menor do que é.
      references: resultados.map(
        (resultado, indice): Reference =>
          resultado.data ?? { id: ids[indice]!, name: ids[indice]! },
      ),
      isPending: resultados.some((resultado) => resultado.isPending),
    }),
  });
}
