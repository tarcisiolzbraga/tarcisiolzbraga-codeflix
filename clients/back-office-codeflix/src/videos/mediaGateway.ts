import { ADMIN_API, MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { AdminApiError } from "@/shared/http";
import type { MediaType } from "@/videos/videosGateway";

// O envio de arquivo **não** passa pelo `adminFetch`: ele põe `Content-Type: application/json` em
// toda chamada com corpo, e aqui o cabeçalho tem de ser `multipart/form-data` **com o boundary** —
// que só o browser sabe gerar. Definir o `Content-Type` à mão aqui quebraria o upload de um jeito
// difícil de ler: o Spring recusaria o corpo por não achar o boundary.
export async function uploadMedia(
  videoId: string,
  type: MediaType,
  file: File,
  token: string,
): Promise<{ videoId: string; type: string }> {
  if (file.size > MEDIA_MAX_FILE_SIZE) {
    throw new AdminApiError(
      413,
      `O arquivo tem ${megabytes(file.size)} e o limite é ${megabytes(MEDIA_MAX_FILE_SIZE)}.`,
    );
  }

  const corpo = new FormData();
  corpo.append("file", file);

  const resposta = await fetch(
    `${ADMIN_API}/videos/${encodeURIComponent(videoId)}/medias/${type}`,
    { method: "POST", headers: { Accept: "application/json", Authorization: `Bearer ${token}` }, body: corpo },
  );

  if (!resposta.ok) {
    throw new AdminApiError(resposta.status, await motivoDe(resposta));
  }
  return (await resposta.json()) as { videoId: string; type: string };
}

// 413 chega **sem corpo** da admin-api (medido contra a API de verdade), então a frase é nossa.
async function motivoDe(resposta: Response): Promise<string> {
  if (resposta.status === 413) {
    return `O arquivo passa do limite de ${megabytes(MEDIA_MAX_FILE_SIZE)} que a API aceita.`;
  }
  try {
    const corpo = (await resposta.json()) as { message?: string };
    return corpo.message?.trim() || "Não foi possível enviar o arquivo.";
  } catch {
    return "Não foi possível enviar o arquivo.";
  }
}

export function megabytes(bytes: number): string {
  return `${(bytes / 1024 ** 2).toFixed(1).replace(".", ",")} MB`;
}
