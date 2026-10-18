// A consulta de listagem é **a mesma** nos quatro agregados: `CategorySearchRequest`,
// `CastMemberSearchRequest`, `GenreSearchRequest` e `VideoSearchRequest` têm os mesmos campos e os
// mesmos padrões. Ela fica aqui para que divergir seja impossível, e não só improvável.
export type SearchQuery = {
  search: string;
  page: number;
  perPage: number;
  sort: string;
  dir: "asc" | "desc";
};

// Os padrões da API. Divergir faria a primeira página do painel não ser a primeira que ela considera
// padrão.
export const SEARCH_DEFAULTS: SearchQuery = {
  search: "",
  page: 0,
  perPage: 10,
  sort: "name",
  dir: "asc",
};

// `search` vazio é omitido em vez de mandado como string vazia: do outro lado ele entra num `like`,
// e mandar o parâmetro vazio é pedir à API um filtro que não se quer.
export function searchPath(resource: string, query: SearchQuery): string {
  const params = new URLSearchParams({
    page: String(query.page),
    perPage: String(query.perPage),
    sort: query.sort,
    dir: query.dir,
  });
  if (query.search.trim() !== "") {
    params.set("search", query.search.trim());
  }
  return `/${resource}?${params.toString()}`;
}

// Filtrar volta para a primeira página: filtrar e continuar na página 4 mostraria "nenhum resultado"
// num filtro que tem resultados.
export function withSearch(query: SearchQuery, search: string): SearchQuery {
  return search === query.search ? query : { ...query, search, page: 0 };
}
