import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "react-oidc-context";
import { explainFailure } from "@/shared/explainFailure";
import { uploadMedia } from "@/videos/mediaGateway";
import {
  nextToSend,
  UploadQueueContext,
  type QueuedUpload,
  type UploadRequest,
} from "@/videos/uploadQueue";

// A fila de envio de mídia.
//
// **Vive acima do roteador**, e não na tela do vídeo: é isso que a torna fila. Enviar cinco arquivos
// de 100 MB leva minutos, e quem está administrando o catálogo não fica parado olhando — vai editar
// outra coisa. Com o estado na tela, sair dela cancelaria os envios.
export function UploadQueueProvider({ children }: { children: React.ReactNode }) {
  const token = useAuth().user?.access_token;
  const client = useQueryClient();
  const [uploads, setUploads] = useState<QueuedUpload[]>([]);
  // Os arquivos ficam fora do estado: `File` não é serializável nem comparável, e guardá-lo no
  // estado faria cada render carregar o conteúdo junto. O id da fila é a chave.
  const arquivos = useRef(new Map<string, File>());

  const atualizar = useCallback((id: string, mudanca: Partial<QueuedUpload>) => {
    setUploads((atuais) =>
      atuais.map((upload) => (upload.id === id ? { ...upload, ...mudanca } : upload)),
    );
  }, []);

  const enqueue = useCallback((requests: UploadRequest[]) => {
    const novos = requests.map((request) => {
      const id = crypto.randomUUID();
      arquivos.current.set(id, request.file);
      return {
        id,
        videoId: request.videoId,
        videoTitle: request.videoTitle,
        type: request.type,
        filename: request.file.name,
        size: request.file.size,
        status: "waiting" as const,
        progress: 0,
      };
    });
    setUploads((atuais) => [...atuais, ...novos]);
  }, []);

  const dismiss = useCallback((id: string) => {
    arquivos.current.delete(id);
    setUploads((atuais) => atuais.filter((upload) => upload.id !== id));
  }, []);

  const clearFinished = useCallback(() => {
    setUploads((atuais) => {
      for (const upload of atuais) {
        if (upload.status === "done" || upload.status === "failed") {
          arquivos.current.delete(upload.id);
        }
      }
      return atuais.filter((upload) => upload.status === "waiting" || upload.status === "sending");
    });
  }, []);

  // O motor da fila: um efeito que, a cada mudança, envia o próximo se não houver nenhum em curso.
  // A decisão de ser **em série** está no `nextToSend`, com o porquê.
  useEffect(() => {
    const proximo = nextToSend(uploads);
    if (proximo === undefined || token === undefined) {
      return;
    }
    const arquivo = arquivos.current.get(proximo.id);
    if (arquivo === undefined) {
      atualizar(proximo.id, { status: "failed", failure: "O arquivo não está mais disponível." });
      return;
    }

    atualizar(proximo.id, { status: "sending" });
    uploadMedia(proximo.videoId, proximo.type, arquivo, token, (fracao) =>
      atualizar(proximo.id, { progress: fracao }),
    )
      .then(() => {
        atualizar(proximo.id, { status: "done", progress: 1 });
        arquivos.current.delete(proximo.id);
        // O vídeo passa a mostrar a mídia nova. Invalida só ele: a listagem não mostra mídia.
        void client.invalidateQueries({ queryKey: ["video", proximo.videoId] });
      })
      .catch((erro: unknown) =>
        atualizar(proximo.id, { status: "failed", failure: explainFailure(erro) }),
      );
  }, [uploads, token, atualizar, client]);

  return (
    <UploadQueueContext value={{ uploads, enqueue, dismiss, clearFinished }}>
      {children}
    </UploadQueueContext>
  );
}
