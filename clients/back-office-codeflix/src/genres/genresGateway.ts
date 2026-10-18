import { adminFetch, type Page } from "@/shared/http";
import { searchPath, type SearchQuery } from "@/shared/search";

// O `GenreListResponse` da admin-api, campo por campo. `categories` são **ids**, não nomes — e isso
// decide a coluna da lista: ver abaixo, em `GenreList`.
export type Genre = {
  id: string;
  name: string;
  categories: string[];
  active: boolean;
  createdAt: string;
};

export type GenreDetail = Genre & { updatedAt: string };

export type GenreDraft = { name: string; categories: string[]; active: boolean };

export function genresPath(query: SearchQuery): string {
  return searchPath("genres", query);
}

export function listGenres(query: SearchQuery, token: string): Promise<Page<Genre>> {
  return adminFetch<Page<Genre>>(genresPath(query), token, {
    fallback: "Não foi possível carregar os gêneros.",
  });
}

export function getGenre(id: string, token: string): Promise<GenreDetail> {
  return adminFetch<GenreDetail>(`/genres/${encodeURIComponent(id)}`, token, {
    fallback: "Não foi possível carregar o gênero.",
  });
}

export function createGenre(draft: GenreDraft, token: string): Promise<{ id: string }> {
  return adminFetch<{ id: string }>("/genres", token, {
    method: "POST",
    body: draft,
    fallback: "Não foi possível criar o gênero.",
  });
}

// `active` fora do corpo, como nos outros dois: ativar e desativar têm rotas próprias.
//
// `categories` **substitui** a lista inteira, e não acrescenta: é o que o `UpdateGenreCommand` faz do
// outro lado. Mandar a lista parcial tiraria as que ficaram de fora.
export function updateGenre(
  id: string,
  draft: Pick<GenreDraft, "name" | "categories">,
  token: string,
): Promise<{ id: string }> {
  return adminFetch<{ id: string }>(`/genres/${encodeURIComponent(id)}`, token, {
    method: "PUT",
    body: draft,
    fallback: "Não foi possível salvar o gênero.",
  });
}

export function setGenreActive(id: string, active: boolean, token: string): Promise<GenreDetail> {
  const rota = active ? "activate" : "deactivate";
  return adminFetch<GenreDetail>(`/genres/${encodeURIComponent(id)}/${rota}`, token, {
    method: "PUT",
    fallback: active ? "Não foi possível ativar." : "Não foi possível desativar.",
  });
}

export function deleteGenre(id: string, token: string): Promise<void> {
  return adminFetch<void>(`/genres/${encodeURIComponent(id)}`, token, {
    method: "DELETE",
    fallback: "Não foi possível remover o gênero.",
  });
}
