import { adminFetch, type Page } from "@/shared/http";

// A categoria como a admin-api a devolve na listagem: o CategoryListResponse de lá, campo por campo.
// `createdAt` fica como string porque é o que o JSON traz (ISO-8601, do Instant) — quem mostra
// decide o formato, e não este arquivo.
export type Category = {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
};

// Os mesmos nomes e padrões do CategorySearchRequest da admin-api. Estão aqui num tipo só para que
// errar um nome seja erro de compilação, e não uma lista que volta sem o filtro pedido.
export type CategoryQuery = {
  search: string;
  page: number;
  perPage: number;
  sort: string;
  dir: "asc" | "desc";
};

export const CATEGORY_QUERY_DEFAULTS: CategoryQuery = {
  search: "",
  page: 0,
  perPage: 10,
  sort: "name",
  dir: "asc",
};

// `search` vazio é omitido em vez de mandado como string vazia: do outro lado ele entra num `like`,
// e mandar o parâmetro vazio é pedir à admin-api um filtro que não se quer.
export function categoriesPath(query: CategoryQuery): string {
  const params = new URLSearchParams({
    page: String(query.page),
    perPage: String(query.perPage),
    sort: query.sort,
    dir: query.dir,
  });
  if (query.search.trim() !== "") {
    params.set("search", query.search.trim());
  }
  return `/categories?${params.toString()}`;
}

export function listCategories(query: CategoryQuery, token: string): Promise<Page<Category>> {
  return adminFetch<Page<Category>>(categoriesPath(query), token, {
    fallback: "Não foi possível carregar as categorias.",
  });
}
