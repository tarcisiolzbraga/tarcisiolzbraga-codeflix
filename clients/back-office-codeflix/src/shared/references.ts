import { adminFetch, type Page } from "@/shared/http";
import { searchPath, type SearchQuery } from "@/shared/search";

// Os três agregados que um vídeo (e um gênero) referenciam têm **a mesma forma** no que importa para
// escolher um: id e nome. Isso é o que permite um campo de escolha só para os três.
export type Reference = { id: string; name: string };

export type ReferenceResource = "categories" | "genres" | "cast-members";

export const REFERENCE_LABELS: Record<ReferenceResource, string> = {
  categories: "Categorias",
  genres: "Gêneros",
  "cast-members": "Elenco",
};

// A chave de cache de um recurso lido por id é a **mesma** que a tela de edição dele usa. Não é
// detalhe: a rota é a mesma (`GET /{recurso}/{id}`) e devolve o mesmo corpo, então abrir um gênero
// depois de abrir uma categoria não refaz a chamada — e o contrário também vale.
export const REFERENCE_QUERY_KEYS: Record<ReferenceResource, string> = {
  categories: "category",
  genres: "genre",
  "cast-members": "castMember",
};

export function searchReferences(
  resource: ReferenceResource,
  query: SearchQuery,
  token: string,
): Promise<Page<Reference>> {
  return adminFetch<Page<Reference>>(searchPath(resource, query), token, {
    fallback: `Não foi possível buscar em ${REFERENCE_LABELS[resource].toLowerCase()}.`,
  });
}

export function getReference(
  resource: ReferenceResource,
  id: string,
  token: string,
): Promise<Reference> {
  return adminFetch<Reference>(`/${resource}/${encodeURIComponent(id)}`, token, {
    fallback: "Não foi possível carregar o registro.",
  });
}
