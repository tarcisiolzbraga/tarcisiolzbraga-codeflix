import { ADMIN_API, MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { AdminApiError } from "@/shared/http";
import type { MediaType } from "@/videos/videosGateway";

// O envio de arquivo **não** passa pelo `adminFetch`: ele põe `Content-Type: application/json` em
// toda chamada com corpo, e aqui o cabeçalho tem de ser `multipart/form-data` **com o boundary** —
// que só o browser sabe gerar. Definir o `Content-Type` à mão aqui quebraria o upload de um jeito
// difícil de ler: o Spring recusaria o corpo por não achar o boundary.
export type UploadResult = { videoId: string; type: string };

// **`XMLHttpRequest`, e não `fetch`.** Não é nostalgia: o `fetch` não expõe o progresso do **envio**.
// Ele tem `response.body` como fluxo para o que chega, e nada para o que sai — a proposta de
// `ReadableStream` como corpo existe, mas não dá acesso a quantos bytes foram aceitos. O
// `xhr.upload.onprogress` dá, e um arquivo de 100 MB sem barra de progresso é uma tela que parece
// travada.
//
// O resto do contrato é o mesmo do `adminFetch`: o `Bearer`, a mensagem de erro da API, e o teto
// conferido antes de qualquer byte sair.
export function uploadMedia(
  videoId: string,
  type: MediaType,
  file: File,
  token: string,
  onProgress?: (fraction: number) => void,
): Promise<UploadResult> {
  if (file.size > MEDIA_MAX_FILE_SIZE) {
    return Promise.reject(
      new AdminApiError(
        413,
        `O arquivo tem ${megabytes(file.size)} e o limite é ${megabytes(MEDIA_MAX_FILE_SIZE)}.`,
      ),
    );
  }

  const corpo = new FormData();
  corpo.append("file", file);

  return new Promise<UploadResult>((resolve, reject) => {
    const pedido = new XMLHttpRequest();
    pedido.open("POST", `${ADMIN_API}/videos/${encodeURIComponent(videoId)}/medias/${type}`);
    pedido.setRequestHeader("Accept", "application/json");
    pedido.setRequestHeader("Authorization", `Bearer ${token}`);
    // Sem `Content-Type` à mão: o browser o monta com o boundary do multipart, como no `fetch`.

    pedido.upload.addEventListener("progress", (evento) => {
      // `lengthComputable` é falso quando o tamanho total não é conhecido — não é o caso de um
      // arquivo escolhido no disco, mas o guarda evita uma divisão por zero se for.
      if (evento.lengthComputable && evento.total > 0) {
        onProgress?.(evento.loaded / evento.total);
      }
    });

    pedido.addEventListener("load", () => {
      if (pedido.status >= 200 && pedido.status < 300) {
        onProgress?.(1);
        resolve(JSON.parse(pedido.responseText) as UploadResult);
        return;
      }
      reject(new AdminApiError(pedido.status, motivoDaResposta(pedido)));
    });

    // Rede caída e envio cancelado chegam como eventos próprios, e não como status.
    pedido.addEventListener("error", () =>
      reject(new AdminApiError(0, "Não foi possível falar com a admin-api. Verifique se ela está no ar.")),
    );
    pedido.addEventListener("abort", () => reject(new AdminApiError(0, "Envio cancelado.")));

    pedido.send(corpo);
  });
}

// 413 chega **sem corpo** da admin-api (medido contra a API de verdade), então a frase é nossa.
function motivoDaResposta(pedido: XMLHttpRequest): string {
  if (pedido.status === 413) {
    return `O arquivo passa do limite de ${megabytes(MEDIA_MAX_FILE_SIZE)} que a API aceita.`;
  }
  try {
    const corpo = JSON.parse(pedido.responseText) as { message?: string };
    return corpo.message?.trim() || "Não foi possível enviar o arquivo.";
  } catch {
    return "Não foi possível enviar o arquivo.";
  }
}

export function megabytes(bytes: number): string {
  return `${(bytes / 1024 ** 2).toFixed(1).replace(".", ",")} MB`;
}
