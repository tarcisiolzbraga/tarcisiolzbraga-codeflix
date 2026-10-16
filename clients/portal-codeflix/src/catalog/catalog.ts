import { CATALOG_API } from "@/shared/config";
import { bearer } from "@/shared/http";

export type Video = {
  id: string;
  title: string;
  description: string;
  yearLaunched: number;
  duration: number;
  rating: string;
  genres: { id: string; name: string }[];
  categories: { id: string; name: string }[];
  castMembers: { id: string; name: string }[];
};

export type VideoPage = {
  meta: { currentPage: number; perPage: number; total: number };
  items: Video[];
};

// O catálogo recusa **dentro do corpo**, com 200 de HTTP: GraphQL responde assim, e a classificação
// vem em `errors[0].extensions.classification`. Tratar só o status esconderia a recusa.
export class CatalogRefused extends Error {
  constructor(readonly classification: "UNAUTHORIZED" | "FORBIDDEN" | string) {
    super(`o catálogo recusou: ${classification}`);
    this.name = "CatalogRefused";
  }
}

export class CatalogUnavailable extends Error {
  constructor(readonly status: number) {
    super(`a videos-api respondeu ${status}`);
    this.name = "CatalogUnavailable";
  }
}

// **FORBIDDEN aqui quase sempre é o papel do catálogo que ainda não chegou ao token.**
//
// A assinatura concede CODEFLIX_SUBSCRIBER por evento, alguns segundos depois; o token em mãos foi
// assinado antes. Quem chama esta função trata esse caso oferecendo renovar a sessão, em vez de
// dizer "acesso negado" a quem acabou de pagar.
export function isMissingCatalogRole(error: unknown): boolean {
  return error instanceof CatalogRefused && error.classification === "FORBIDDEN";
}

type GraphQlAnswer<T> = {
  data?: T;
  errors?: { message: string; extensions?: { classification?: string } }[];
};

async function query<T>(document: string, variables: Record<string, unknown>, token: string): Promise<T> {
  const response = await fetch(`${CATALOG_API}/api/graphql`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json", ...bearer(token) },
    body: JSON.stringify({ query: document, variables }),
  });

  // Token inválido é recusado **antes** do GraphQL existir, e aí a resposta é HTTP mesmo.
  if (!response.ok) {
    throw new CatalogUnavailable(response.status);
  }

  const answer = (await response.json()) as GraphQlAnswer<T>;
  const refusal = answer.errors?.[0]?.extensions?.classification;
  if (refusal) {
    throw new CatalogRefused(refusal);
  }
  if (answer.errors?.length) {
    throw new Error(answer.errors[0]!.message);
  }
  if (!answer.data) {
    throw new CatalogUnavailable(response.status);
  }
  return answer.data;
}

const VIDEO_FIELDS = `
  id title description yearLaunched duration rating
  genres { id name }
  categories { id name }
  castMembers { id name }`;

export async function searchVideos(
  { search, page }: { search: string; page: number },
  token: string,
): Promise<VideoPage> {
  const data = await query<{ videos: VideoPage }>(
    `query Acervo($search: String, $page: Int) {
       videos(search: $search, page: $page, perPage: 12) {
         meta { currentPage perPage total }
         items {${VIDEO_FIELDS} }
       }
     }`,
    { search, page },
    token,
  );
  return data.videos;
}

// **Nulo quando o vídeo não é servido aqui**, como o schema diz: o acervo só mostra o que está
// publicado e ativo, então um id válido pode simplesmente não ter nada para devolver.
export async function fetchVideo(id: string, token: string): Promise<Video | null> {
  const data = await query<{ video: Video | null }>(
    `query Video($id: ID!) { video(id: $id) {${VIDEO_FIELDS} } }`,
    { id },
    token,
  );
  return data.video;
}

// A duração vem em minutos, como ponto flutuante. "1h 47min" é o que se lê numa tela de catálogo;
// "107.0" não é.
export function formatDuration(minutes: number): string {
  const total = Math.round(minutes);
  const hours = Math.floor(total / 60);
  const rest = total % 60;
  if (hours === 0) {
    return `${rest}min`;
  }
  return rest === 0 ? `${hours}h` : `${hours}h ${rest}min`;
}

// Os rótulos que o admin publica: ER, L, 10, 12, 14, 16, 18. Mostrados como estão, porque é o que a
// classificação indicativa significa no Brasil — traduzir inventaria uma segunda nomenclatura.
export function ratingLabel(rating: string): string {
  return rating === "L" ? "Livre" : rating === "ER" ? "Especialmente recomendado" : `${rating} anos`;
}
