import { beforeEach, describe, expect, it } from "vitest";
import { ADMIN_API, MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { AdminApiError } from "@/shared/http";
import { installXhrDouble, type XhrDouble } from "@/shared/testing";
import { megabytes, uploadMedia } from "@/videos/mediaGateway";

function arquivoDe(bytes: number, nome = "duna.mp4"): File {
  return new File([new Uint8Array(bytes)], nome, { type: "video/mp4" });
}

describe("uploadMedia", () => {
  let xhr: XhrDouble;

  beforeEach(() => {
    xhr = installXhrDouble();
  });

  it("givenUmArquivo_whenEnviado_thenVaiDePostNaRotaDaMidia", async () => {
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");
    xhr.respond(0, 201, JSON.stringify({ videoId: "vid-1", type: "VIDEO" }));

    await expect(enviando).resolves.toEqual({ videoId: "vid-1", type: "VIDEO" });
    expect(xhr.sent[0]!.method).toBe("POST");
    expect(xhr.sent[0]!.url).toBe(`${ADMIN_API}/videos/vid-1/medias/VIDEO`);
  });

  it("givenUmArquivo_whenEnviado_thenOCorpoEhFormDataComAParteFile", async () => {
    const enviando = uploadMedia("vid-1", "BANNER", arquivoDe(10, "banner.png"), "abc.123");
    xhr.respond(0, 201);

    await enviando;
    const corpo = xhr.sent[0]!.body;
    expect(corpo).toBeInstanceOf(FormData);
    expect((corpo as FormData).get("file")).toBeInstanceOf(File);
  });

  // **Sem `Content-Type` à mão.** Ele tem de ser `multipart/form-data` **com o boundary**, que só o
  // browser gera; definir o cabeçalho faria o Spring recusar o corpo por não achar o boundary.
  it("givenUmArquivo_whenEnviado_thenNaoDefineContentTypeEMandaOBearer", async () => {
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");
    xhr.respond(0, 201);

    await enviando;
    expect(xhr.sent[0]!.headers["Content-Type"]).toBeUndefined();
    expect(xhr.sent[0]!.headers.Authorization).toBe("Bearer abc.123");
  });

  // **O que o `fetch` não dá:** o progresso do envio. É por isto que esta peça usa XHR.
  it("givenUmEnvioEmCurso_whenOBrowserReportaProgresso_thenEleEhRepassadoComoFracao", async () => {
    const fracoes: number[] = [];
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123", (fracao) =>
      fracoes.push(fracao),
    );

    xhr.progress(0, 25, 100);
    xhr.progress(0, 80, 100);
    xhr.respond(0, 201);
    await enviando;

    // 1 no fim, mesmo que o browser não emita o último progresso: a barra não pode ficar em 80%
    // depois de a resposta ter chegado.
    expect(fracoes).toEqual([0.25, 0.8, 1]);
  });

  // O teto é conferido **antes** de qualquer byte sair, porque a API responde 413 sem corpo.
  it("givenArquivoAcimaDoTeto_whenEnviado_thenNemAbreOPedido", async () => {
    const erro = (await uploadMedia(
      "vid-1",
      "VIDEO",
      arquivoDe(MEDIA_MAX_FILE_SIZE + 1),
      "abc.123",
    ).catch((e: unknown) => e)) as AdminApiError;

    expect(erro).toBeInstanceOf(AdminApiError);
    expect(erro.status).toBe(413);
    expect(xhr.sent).toHaveLength(0);
  });

  it("givenArquivoExatamenteNoTeto_whenEnviado_thenVai", async () => {
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(MEDIA_MAX_FILE_SIZE), "abc.123");
    xhr.respond(0, 201);

    await enviando;
    expect(xhr.sent).toHaveLength(1);
  });

  // 413 da API chega **sem corpo** (medido contra a admin-api de verdade), então a frase é nossa.
  it("given413SemCorpo_whenEnviado_thenExplicaOLimite", async () => {
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");
    xhr.respond(0, 413, "");

    const erro = (await enviando.catch((e: unknown) => e)) as AdminApiError;
    expect(erro.status).toBe(413);
    expect(erro.message).toContain("limite");
  });

  it("givenOutraFalhaComMensagem_whenEnviado_thenRepeteAMensagemDaApi", async () => {
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");
    xhr.respond(0, 404, JSON.stringify({ message: "Video with ID vid-1 was not found" }));

    const erro = (await enviando.catch((e: unknown) => e)) as AdminApiError;
    expect(erro.message).toBe("Video with ID vid-1 was not found");
  });

  // Rede caída chega como evento `error`, e não como status: sem este ramo a promessa nunca
  // resolveria, e o envio ficaria "enviando" para sempre na fila.
  it("givenRedeCaida_whenEnviado_thenFalhaDizendoQueNaoFalouComAApi", async () => {
    const enviando = uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");
    xhr.fail(0);

    const erro = (await enviando.catch((e: unknown) => e)) as AdminApiError;
    expect(erro.message).toContain("admin-api");
  });
});

describe("megabytes", () => {
  it("givenBytes_whenFormatados_thenSaemEmMegabytesComVirgula", () => {
    expect(megabytes(1024 ** 2 * 1.5)).toBe("1,5 MB");
    expect(megabytes(1024 ** 2 * 100)).toBe("100,0 MB");
  });
});
