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

// A categoria inteira, como o `CategoryResponse` da admin-api a devolve: a listagem não traz
// `updatedAt`, e a tela de edição carrega por aqui em vez de reaproveitar a linha da tabela — a
// linha pode estar velha, e salvar por cima do que outra pessoa mudou é pior que uma ida à rede.
export type CategoryDetail = Category & { updatedAt: string };

export function getCategory(id: string, token: string): Promise<CategoryDetail> {
  return adminFetch<CategoryDetail>(`/categories/${encodeURIComponent(id)}`, token, {
    fallback: "Não foi possível carregar a categoria.",
  });
}

// O que o formulário manda. `active` só existe na criação: no `UpdateCategoryRequest` da admin-api
// ele **não** existe, porque ativar e desativar têm rotas próprias — e repetir o campo aqui faria a
// tela mandar algo que o outro lado ignora em silêncio.
export type CategoryDraft = { name: string; description: string | null; active: boolean };

export function createCategory(draft: CategoryDraft, token: string): Promise<{ id: string }> {
  return adminFetch<{ id: string }>("/categories", token, {
    method: "POST",
    body: draft,
    fallback: "Não foi possível criar a categoria.",
  });
}

export function updateCategory(
  id: string,
  draft: Pick<CategoryDraft, "name" | "description">,
  token: string,
): Promise<{ id: string }> {
  return adminFetch<{ id: string }>(`/categories/${encodeURIComponent(id)}`, token, {
    method: "PUT",
    body: draft,
    fallback: "Não foi possível salvar a categoria.",
  });
}

// Uma função para os dois lados, porque as rotas só diferem no verbo final e as duas são
// idempotentes do lado da API — reativar o que já está ativo responde 200.
export function setCategoryActive(
  id: string,
  active: boolean,
  token: string,
): Promise<CategoryDetail> {
  const rota = active ? "activate" : "deactivate";
  return adminFetch<CategoryDetail>(`/categories/${encodeURIComponent(id)}/${rota}`, token, {
    method: "PUT",
    fallback: active ? "Não foi possível ativar a categoria." : "Não foi possível desativar.",
  });
}

export function deleteCategory(id: string, token: string): Promise<void> {
  return adminFetch<void>(`/categories/${encodeURIComponent(id)}`, token, {
    method: "DELETE",
    fallback: "Não foi possível remover a categoria.",
  });
}
