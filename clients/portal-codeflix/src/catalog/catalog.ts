import { CATALOG_API } from "@/shared/config";
import { bearer } from "@/shared/http";

export type Video = {
  id: string;
  title: string;
  description: string;
  yearLaunched: number;
  duration: number;
  rating: string;
  // Os campos de mídia guardam a **localização no armazenamento**, não uma URL que o browser
  // alcance — é o que o schema do catálogo diz, e o que o admin publica. Eles servem para uma coisa
  // só aqui: saber se o título **tem** aquela arte, antes de pedir o endereço assinado dela.
  //
  // Nulo quando o arquivo não foi enviado, e é caso comum: um título recém-cadastrado não tem nada.
  // Os dois primeiros são arquivo de áudio e vídeo; os três seguintes, imagem.
  video: string | null;
  trailer: string | null;
  banner: string | null;
  thumbnail: string | null;
  thumbnailHalf: string | null;
  genres: { id: string; name: string }[];
  categories: { id: string; name: string }[];
  castMembers: { id: string; name: string }[];
};

// Os cinco tipos que a rota de mídia do catálogo repassa, com os nomes do `VideoMediaType` do admin.
export type MediaType = "VIDEO" | "TRAILER" | "BANNER" | "THUMBNAIL" | "THUMBNAIL_HALF";

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
  video trailer banner thumbnail thumbnailHalf
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

// O endereço **assinado** da mídia, pedido no clique.
//
// Existe porque `<video src>` não carrega cabeçalho de `Authorization`: o browser busca o `src` por
// conta própria, sem token. O catálogo emite um caminho com validade e assinatura depois de conferir
// quem está pedindo, e o player aponta para ele — reprodução progressiva, sem baixar o arquivo antes.
//
// A primeira versão desta tela fazia o contrário: baixava os bytes com `fetch` e tocava de um
// endereço de objeto local. Funcionava, e custava a reprodução progressiva e a busca na linha de
// tempo.
export type SignedMedia = { url: string; expiresAt: string };

export async function fetchSignedMediaUrl(
  id: string,
  type: MediaType,
  token: string,
): Promise<SignedMedia | null> {
  const response = await fetch(`${CATALOG_API}/api/videos/${id}/medias/${type}/link`, {
    headers: { Accept: "application/json", ...bearer(token) },
  });

  // 404 aqui seria vídeo inexistente; a ausência de **arquivo** só aparece ao buscar a mídia, e a
  // tela trata os dois como "não tem o que reproduzir".
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new CatalogUnavailable(response.status);
  }

  const body = (await response.json()) as { path: string; expiresAt: string };
  // O caminho vem **relativo** de propósito: quem sabe o host público do catálogo é quem publica o
  // serviço, não ele. Quem junta é o cliente, que já conhece a base para falar com a API.
  return { url: `${CATALOG_API}/api${body.path}`, expiresAt: body.expiresAt };
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
