import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API, MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { AdminApiError } from "@/shared/http";
import { jsonResponse } from "@/shared/testing";
import { megabytes, uploadMedia } from "@/videos/mediaGateway";

function arquivoDe(bytes: number, nome = "duna.mp4"): File {
  return new File([new Uint8Array(bytes)], nome, { type: "video/mp4" });
}

describe("uploadMedia", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ videoId: "vid-1", type: "VIDEO" })),
    );
  });

  function chamada() {
    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    return { url, init: init as RequestInit };
  }

  it("givenUmArquivo_whenEnviado_thenVaiDePostNaRotaDaMidia", async () => {
    await uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/videos/vid-1/medias/VIDEO`);
    expect(chamada().init.method).toBe("POST");
  });

  it("givenUmArquivo_whenEnviado_thenOCorpoEhFormDataComAParteFile", async () => {
    await uploadMedia("vid-1", "BANNER", arquivoDe(10, "banner.png"), "abc.123");

    const corpo = chamada().init.body;
    expect(corpo).toBeInstanceOf(FormData);
    expect((corpo as FormData).get("file")).toBeInstanceOf(File);
  });

  // **Sem `Content-Type` à mão.** Ele tem de ser `multipart/form-data` com o boundary, que só o
  // browser gera; definir o cabeçalho aqui faria o Spring recusar o corpo por não achar o boundary.
  it("givenUmArquivo_whenEnviado_thenNaoDefineContentType", async () => {
    await uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123");

    const headers = chamada().init.headers as Record<string, string>;
    expect(headers["Content-Type"]).toBeUndefined();
    expect(headers.Authorization).toBe("Bearer abc.123");
  });

  // O teto é conferido **antes** de enviar, porque a API responde 413 sem corpo: sem isso a pessoa
  // esperaria o upload inteiro para ler "falhou" sem motivo.
  it("givenArquivoAcimaDoTeto_whenEnviado_thenNemChamaAApi", async () => {
    const erro = (await uploadMedia(
      "vid-1",
      "VIDEO",
      arquivoDe(MEDIA_MAX_FILE_SIZE + 1),
      "abc.123",
    ).catch((e: unknown) => e)) as AdminApiError;

    expect(erro).toBeInstanceOf(AdminApiError);
    expect(erro.status).toBe(413);
    expect(erro.message).toContain("o limite é");
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenArquivoExatamenteNoTeto_whenEnviado_thenVai", async () => {
    await uploadMedia("vid-1", "VIDEO", arquivoDe(MEDIA_MAX_FILE_SIZE), "abc.123");

    expect(globalThis.fetch).toHaveBeenCalledOnce();
  });

  // 413 da API chega **sem corpo** (medido contra a admin-api de verdade), então a frase é nossa.
  it("given413SemCorpo_whenEnviado_thenExplicaOLimite", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(undefined, { status: 413 })),
    );

    const erro = (await uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123").catch(
      (e: unknown) => e,
    )) as AdminApiError;

    expect(erro.status).toBe(413);
    expect(erro.message).toContain("limite");
  });

  it("givenOutraFalhaComMensagem_whenEnviado_thenRepeteAMensagemDaApi", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse({ message: "Video with ID vid-1 was not found" }, { status: 404 })),
    );

    const erro = (await uploadMedia("vid-1", "VIDEO", arquivoDe(10), "abc.123").catch(
      (e: unknown) => e,
    )) as AdminApiError;

    expect(erro.message).toBe("Video with ID vid-1 was not found");
  });
});

describe("megabytes", () => {
  it("givenBytes_whenFormatados_thenSaemEmMegabytesComVirgula", () => {
    expect(megabytes(1024 ** 2 * 1.5)).toBe("1,5 MB");
    expect(megabytes(1024 ** 2 * 100)).toBe("100,0 MB");
  });
});
