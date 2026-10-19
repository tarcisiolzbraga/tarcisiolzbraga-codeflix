import { describe, expect, it } from "vitest";
import { MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { sendable } from "@/videos/chosenMedia";

function arquivo(bytes: number, nome: string): File {
  return new File([new Uint8Array(bytes)], nome);
}

describe("sendable", () => {
  it("givenNadaEscolhido_whenFiltrado_thenNaoHaNadaAEnviar", () => {
    expect(sendable({})).toEqual([]);
  });

  // O que passa do teto fica de fora: a API o recusaria com 413, e a fila mostraria um erro que já
  // era conhecido na tela anterior.
  it("givenArquivoAcimaDoTeto_whenFiltrado_thenEleNaoVai", () => {
    const grande = arquivo(MEDIA_MAX_FILE_SIZE + 1, "grande.mp4");

    expect(sendable({ VIDEO: grande })).toEqual([]);
  });

  it("givenArquivoNoTeto_whenFiltrado_thenEleVai", () => {
    const limite = arquivo(MEDIA_MAX_FILE_SIZE, "limite.mp4");

    expect(sendable({ VIDEO: limite }).map((e) => e.type)).toEqual(["VIDEO"]);
  });

  // A ordem é a do `MEDIA_TYPES`, e não a de escolha: assim a fila sai sempre igual, e o vídeo — que
  // é o arquivo grande — vai primeiro.
  it("givenVariosEscolhidos_whenFiltrados_thenSaemNaOrdemDosTipos", () => {
    const escolhidos = {
      THUMBNAIL: arquivo(1, "t.png"),
      VIDEO: arquivo(1, "v.mp4"),
      BANNER: arquivo(1, "b.png"),
    };

    expect(sendable(escolhidos).map((e) => e.type)).toEqual(["VIDEO", "BANNER", "THUMBNAIL"]);
  });
});
