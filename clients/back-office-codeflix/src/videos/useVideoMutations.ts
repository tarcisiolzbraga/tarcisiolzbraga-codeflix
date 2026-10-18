import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import {
  changeVideoState,
  createVideo,
  deleteVideo,
  getVideo,
  updateVideo,
  type MediaType,
  type VideoDraft,
  type VideoStateChange,
} from "@/videos/videosGateway";
import { uploadMedia } from "@/videos/mediaGateway";

function useToken(): string | undefined {
  return useAuth().user?.access_token;
}

export function useVideo(id: string | undefined) {
  const token = useToken();

  return useQuery({
    queryKey: ["video", id],
    queryFn: () => getVideo(id!, token!),
    enabled: id !== undefined && token !== undefined,
  });
}

function useInvalidate() {
  const client = useQueryClient();
  return (id?: string) => {
    void client.invalidateQueries({ queryKey: ["videos"] });
    if (id !== undefined) {
      void client.invalidateQueries({ queryKey: ["video", id] });
    }
  };
}

export function useCreateVideo() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (draft: VideoDraft) => createVideo(draft, token!),
    onSuccess: () => invalidar(),
  });
}

export function useUpdateVideo(id: string) {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (draft: VideoDraft) => updateVideo(id, draft, token!),
    onSuccess: () => invalidar(id),
  });
}

// Uma mutação para as **seis** rotas de estado: elas só diferem no verbo final, e todas devolvem o
// vídeo inteiro. Separá-las em seis hooks repetiria a invalidação seis vezes.
export function useChangeVideoState() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: ({ id, change }: { id: string; change: VideoStateChange }) =>
      changeVideoState(id, change, token!),
    onSuccess: (_, { id }) => invalidar(id),
  });
}

export function useDeleteVideo() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (id: string) => deleteVideo(id, token!),
    onSuccess: () => invalidar(),
  });
}

// O envio de arquivo invalida **o vídeo**, e não a listagem: a tabela não mostra mídia, e o detalhe
// passa a mostrar o arquivo recém-enviado.
export function useUploadMedia(id: string) {
  const token = useToken();
  const client = useQueryClient();

  return useMutation({
    mutationFn: ({ type, file }: { type: MediaType; file: File }) =>
      uploadMedia(id, type, file, token!),
    onSuccess: () => void client.invalidateQueries({ queryKey: ["video", id] }),
  });
}
