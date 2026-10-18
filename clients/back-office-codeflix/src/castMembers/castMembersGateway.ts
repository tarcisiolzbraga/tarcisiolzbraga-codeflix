import { adminFetch, type Page } from "@/shared/http";
import { searchPath, type SearchQuery } from "@/shared/search";

// Os dois papéis do `CastMemberType` do domínio. O enum fica aqui como união de texto, e não como
// `enum` do TypeScript: o `erasableSyntaxOnly` proíbe a sintaxe que só o TypeScript tem, e a união
// cobre o mesmo — com a vantagem de ser o próprio texto que viaja no JSON.
export const CAST_MEMBER_TYPES = ["ACTOR", "DIRECTOR"] as const;

export type CastMemberType = (typeof CAST_MEMBER_TYPES)[number];

// Em português só na tela: o que vai e volta da API é `ACTOR` e `DIRECTOR`.
export const CAST_MEMBER_TYPE_LABELS: Record<CastMemberType, string> = {
  ACTOR: "Ator",
  DIRECTOR: "Diretor",
};

// O `CastMemberListResponse` da admin-api, campo por campo. `type` vem como texto porque é assim que
// ela o serializa — e a API o aceita em minúsculas na entrada, mas sempre devolve em maiúsculas.
export type CastMember = {
  id: string;
  name: string;
  type: CastMemberType;
  active: boolean;
  createdAt: string;
};

export type CastMemberDetail = CastMember & { updatedAt: string };

export type CastMemberDraft = { name: string; type: CastMemberType; active: boolean };

export function castMembersPath(query: SearchQuery): string {
  return searchPath("cast-members", query);
}

export function listCastMembers(query: SearchQuery, token: string): Promise<Page<CastMember>> {
  return adminFetch<Page<CastMember>>(castMembersPath(query), token, {
    fallback: "Não foi possível carregar o elenco.",
  });
}

export function getCastMember(id: string, token: string): Promise<CastMemberDetail> {
  return adminFetch<CastMemberDetail>(`/cast-members/${encodeURIComponent(id)}`, token, {
    fallback: "Não foi possível carregar o membro de elenco.",
  });
}

export function createCastMember(
  draft: CastMemberDraft,
  token: string,
): Promise<{ id: string }> {
  return adminFetch<{ id: string }>("/cast-members", token, {
    method: "POST",
    body: draft,
    fallback: "Não foi possível criar o membro de elenco.",
  });
}

// `active` fora do corpo, como na categoria: no `UpdateCastMemberRequest` o campo não existe, porque
// ativar e desativar têm rotas próprias.
export function updateCastMember(
  id: string,
  draft: Pick<CastMemberDraft, "name" | "type">,
  token: string,
): Promise<{ id: string }> {
  return adminFetch<{ id: string }>(`/cast-members/${encodeURIComponent(id)}`, token, {
    method: "PUT",
    body: draft,
    fallback: "Não foi possível salvar o membro de elenco.",
  });
}

export function setCastMemberActive(
  id: string,
  active: boolean,
  token: string,
): Promise<CastMemberDetail> {
  const rota = active ? "activate" : "deactivate";
  return adminFetch<CastMemberDetail>(`/cast-members/${encodeURIComponent(id)}/${rota}`, token, {
    method: "PUT",
    fallback: active ? "Não foi possível ativar." : "Não foi possível desativar.",
  });
}

export function deleteCastMember(id: string, token: string): Promise<void> {
  return adminFetch<void>(`/cast-members/${encodeURIComponent(id)}`, token, {
    method: "DELETE",
    fallback: "Não foi possível remover o membro de elenco.",
  });
}
