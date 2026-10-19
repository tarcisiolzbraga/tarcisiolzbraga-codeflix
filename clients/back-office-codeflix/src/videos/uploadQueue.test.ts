import { describe, expect, it } from "vitest";
import { nextToSend, type QueuedUpload } from "@/videos/uploadQueue";

function upload(status: QueuedUpload["status"], id: string): QueuedUpload {
  return {
    id,
    videoId: "vid-1",
    videoTitle: "Duna",
    type: "VIDEO",
    filename: `${id}.mp4`,
    size: 10,
    status,
    progress: 0,
  };
}

// **A fila é em série, e isso é a decisão que este arquivo guarda.**
//
// Cinco arquivos de 100 MB em paralelo disputam a banda de subida, e o efeito é que nenhum termina
// até perto do fim — a barra de progresso deixa de informar. Em série, o primeiro termina primeiro.
describe("nextToSend", () => {
  it("givenFilaVazia_whenConsultada_thenNaoHaNadaAEnviar", () => {
    expect(nextToSend([])).toBeUndefined();
  });

  it("givenApenasEsperando_whenConsultada_thenDevolveOPrimeiro", () => {
    expect(nextToSend([upload("waiting", "a"), upload("waiting", "b")])?.id).toBe("a");
  });

  // O ponto: com um em curso, ninguém mais sai.
  it("givenUmEmCurso_whenConsultada_thenNaoDevolveOutro", () => {
    expect(nextToSend([upload("sending", "a"), upload("waiting", "b")])).toBeUndefined();
  });

  it("givenOQueTerminou_whenConsultada_thenEleNaoVolta", () => {
    expect(nextToSend([upload("done", "a"), upload("failed", "b")])).toBeUndefined();
  });

  // O que terminou não bloqueia a fila, diferente do que está em curso.
  it("givenTerminadoEEsperando_whenConsultada_thenDevolveOQueEspera", () => {
    expect(nextToSend([upload("done", "a"), upload("waiting", "b")])?.id).toBe("b");
  });
});
