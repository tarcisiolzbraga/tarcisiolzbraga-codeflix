import { createContext, useContext } from "react";
import type { MediaType } from "@/videos/videosGateway";

// Um envio na fila. O `id` é próprio, e não o par vídeo+tipo: substituir o banner de um vídeo
// enquanto o anterior ainda sobe é caso real, e os dois precisam aparecer.
export type QueuedUpload = {
  id: string;
  videoId: string;
  videoTitle: string;
  type: MediaType;
  filename: string;
  size: number;
  status: "waiting" | "sending" | "done" | "failed";
  // Fração de 0 a 1. Fica em 0 enquanto espera.
  progress: number;
  failure?: string;
};

export type UploadRequest = {
  videoId: string;
  videoTitle: string;
  type: MediaType;
  file: File;
};

export type UploadQueue = {
  uploads: QueuedUpload[];
  enqueue: (requests: UploadRequest[]) => void;
  dismiss: (id: string) => void;
  clearFinished: () => void;
};

// O contexto fica em arquivo próprio, separado do provedor, porque um módulo que exporta componente
// e função perde o fast refresh do Vite.
export const UploadQueueContext = createContext<UploadQueue | null>(null);

export function useUploadQueue(): UploadQueue {
  const fila = useContext(UploadQueueContext);
  if (fila === null) {
    throw new Error("useUploadQueue precisa do <UploadQueueProvider> acima dele");
  }
  return fila;
}

// **Um envio por vez, e não todos em paralelo.**
//
// Cinco arquivos de 100 MB em paralelo disputam a banda de subida, e o efeito é que nenhum termina
// até perto do fim — a barra de progresso deixa de informar. Em série, o primeiro termina primeiro, e
// quem está olhando sabe onde está.
export function nextToSend(uploads: QueuedUpload[]): QueuedUpload | undefined {
  if (uploads.some((upload) => upload.status === "sending")) {
    return undefined;
  }
  return uploads.find((upload) => upload.status === "waiting");
}
