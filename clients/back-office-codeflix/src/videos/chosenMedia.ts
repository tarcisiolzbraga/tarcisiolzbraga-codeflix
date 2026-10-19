import { MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { MEDIA_TYPES, type MediaType } from "@/videos/videosGateway";

// Os arquivos escolhidos na tela de criação, antes de o vídeo existir. Em arquivo próprio porque um
// módulo que exporta componente e função perde o fast refresh do Vite.
export type ChosenMedia = Partial<Record<MediaType, File>>;

// O que pode ir para a fila: o que passa do teto fica de fora, porque a API o recusaria com 413 e a
// fila mostraria um erro que já era conhecido na tela anterior.
//
// A ordem é a do `MEDIA_TYPES`, e não a de escolha: assim a fila sai sempre na mesma ordem, e o
// vídeo — que é o arquivo grande — vai primeiro.
export function sendable(chosen: ChosenMedia): { type: MediaType; file: File }[] {
  return MEDIA_TYPES.flatMap((type) => {
    const file = chosen[type];
    return file !== undefined && file.size <= MEDIA_MAX_FILE_SIZE ? [{ type, file }] : [];
  });
}
