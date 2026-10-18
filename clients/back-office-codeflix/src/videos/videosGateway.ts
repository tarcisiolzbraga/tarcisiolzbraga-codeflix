import { adminFetch, type Page } from "@/shared/http";
import { searchPath, type SearchQuery } from "@/shared/search";

// A classificação indicativa, como o `Rating` do domínio a expõe: o **rótulo**, e não o nome da
// constante Java — "10" e "12" não podem ser nome de constante lá, e é o rótulo que viaja no JSON.
export const RATINGS = ["ER", "L", "10", "12", "14", "16", "18"] as const;

export type Rating = (typeof RATINGS)[number];

// Os cinco arquivos que um vídeo carrega: dois de áudio e vídeo, três de imagem.
export const MEDIA_TYPES = ["VIDEO", "TRAILER", "BANNER", "THUMBNAIL", "THUMBNAIL_HALF"] as const;

export type MediaType = (typeof MEDIA_TYPES)[number];

export const MEDIA_LABELS: Record<MediaType, string> = {
  VIDEO: "Vídeo",
  TRAILER: "Trailer",
  BANNER: "Banner",
  THUMBNAIL: "Miniatura",
  THUMBNAIL_HALF: "Miniatura (meia)",
};

// O `VideoListResponse` da admin-api é **estreito** de propósito: nem duração, nem classificação,
// nem os vínculos. A tabela mostra o que ele traz, e o resto está na tela de edição.
export type VideoRow = {
  id: string;
  title: string;
  launchedAt: number;
  published: boolean;
  active: boolean;
  createdAt: string;
};

export type AudioVideoMedia = {
  checksum: string;
  name: string;
  rawLocation: string;
  encodedLocation: string;
  status: string;
};

export type ImageMedia = { checksum: string; name: string; location: string };

// O `VideoResponse` é plano, e os campos de mídia vêm **nulos** quando o arquivo não foi enviado — o
// campo continua no contrato.
export type VideoDetail = {
  id: string;
  title: string;
  description: string;
  launchedAt: number;
  duration: number;
  rating: Rating;
  opened: boolean;
  published: boolean;
  active: boolean;
  categories: string[];
  genres: string[];
  castMembers: string[];
  video: AudioVideoMedia | null;
  trailer: AudioVideoMedia | null;
  banner: ImageMedia | null;
  thumbnail: ImageMedia | null;
  thumbnailHalf: ImageMedia | null;
  createdAt: string;
  updatedAt: string;
};

export type VideoDraft = {
  title: string;
  description: string;
  launchedAt: number;
  duration: number;
  rating: Rating;
  categories: string[];
  genres: string[];
  castMembers: string[];
};

// O vídeo ordena por **`title`**, e não por `name`: é o padrão do `VideoSearchRequest`, e o único
// dos quatro agregados que difere. Copiar `name` daria 500 ou ordem errada, porque a coluna não
// existe.
export const VIDEO_SEARCH_DEFAULTS: SearchQuery = {
  search: "",
  page: 0,
  perPage: 10,
  sort: "title",
  dir: "asc",
};

// Os filtros por vínculo são **repetição do mesmo parâmetro**, como o Spring os lê num `Set<String>`.
export type VideoFilters = {
  categories: string[];
  genres: string[];
  castMembers: string[];
};

export const VIDEO_FILTERS_EMPTY: VideoFilters = { categories: [], genres: [], castMembers: [] };

export function videosPath(query: SearchQuery, filters: VideoFilters): string {
  const base = searchPath("videos", query);
  const extras = new URLSearchParams();
  for (const [campo, ids] of Object.entries(filters)) {
    for (const id of ids) {
      extras.append(campo, id);
    }
  }
  const cauda = extras.toString();
  return cauda === "" ? base : `${base}&${cauda}`;
}

export function listVideos(
  query: SearchQuery,
  filters: VideoFilters,
  token: string,
): Promise<Page<VideoRow>> {
  return adminFetch<Page<VideoRow>>(videosPath(query, filters), token, {
    fallback: "Não foi possível carregar os vídeos.",
  });
}

export function getVideo(id: string, token: string): Promise<VideoDetail> {
  return adminFetch<VideoDetail>(`/videos/${encodeURIComponent(id)}`, token, {
    fallback: "Não foi possível carregar o vídeo.",
  });
}

export function createVideo(draft: VideoDraft, token: string): Promise<{ id: string }> {
  return adminFetch<{ id: string }>("/videos", token, {
    method: "POST",
    body: draft,
    fallback: "Não foi possível criar o vídeo.",
  });
}

export function updateVideo(id: string, draft: VideoDraft, token: string): Promise<{ id: string }> {
  return adminFetch<{ id: string }>(`/videos/${encodeURIComponent(id)}`, token, {
    method: "PUT",
    body: draft,
    fallback: "Não foi possível salvar o vídeo.",
  });
}

// **Três estados independentes, e seis rotas.** `active`, `published` e `opened` são coisas
// diferentes, e nenhum deles passa pelo corpo da atualização:
//
// - `active`   — o registro existe no catálogo;
// - `published`— o vídeo aparece para quem navega;
// - `opened`   — o vídeo pode ser assistido **sem assinatura**.
export type VideoStateChange = "publish" | "unpublish" | "open" | "close" | "activate" | "deactivate";

const FALHAS: Record<VideoStateChange, string> = {
  publish: "Não foi possível publicar o vídeo.",
  unpublish: "Não foi possível despublicar o vídeo.",
  open: "Não foi possível abrir o vídeo.",
  close: "Não foi possível fechar o vídeo.",
  activate: "Não foi possível ativar o vídeo.",
  deactivate: "Não foi possível desativar o vídeo.",
};

export function changeVideoState(
  id: string,
  change: VideoStateChange,
  token: string,
): Promise<VideoDetail> {
  return adminFetch<VideoDetail>(`/videos/${encodeURIComponent(id)}/${change}`, token, {
    method: "PUT",
    fallback: FALHAS[change],
  });
}

export function deleteVideo(id: string, token: string): Promise<void> {
  return adminFetch<void>(`/videos/${encodeURIComponent(id)}`, token, {
    method: "DELETE",
    fallback: "Não foi possível remover o vídeo.",
  });
}
