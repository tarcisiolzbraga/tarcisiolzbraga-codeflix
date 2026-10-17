import { beforeEach, describe, expect, it, vi } from "vitest";
import { AdminApiError, adminFetch } from "@/shared/http";
import { ADMIN_API } from "@/shared/config";

function respondeCom(body: unknown, init: ResponseInit = {}) {
  vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    // Uma `Response` nova por chamada: reaproveitar a mesma instância faz a segunda leitura estourar
    // com "body already read", e o teste acusa um defeito que não existe.
    Promise.resolve(
      body === undefined
        ? new Response(null, init)
        : new Response(JSON.stringify(body), {
            ...init,
            headers: { "Content-Type": "application/json" },
          }),
    ),
  );
}

describe("adminFetch", () => {
  beforeEach(() => {
    respondeCom({ ok: true });
  });

  it("givenUmToken_whenChama_thenMandaOBearerEOAccept", async () => {
    await adminFetch("/categories", "abc.123", { fallback: "falhou" });

    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    expect(url).toBe(`${ADMIN_API}/categories`);
    const headers = (init as RequestInit).headers as Record<string, string>;
    expect(headers.Authorization).toBe("Bearer abc.123");
    expect(headers.Accept).toBe("application/json");
  });

  it("givenChamadaSemCorpo_whenChama_thenNaoMandaContentType", async () => {
    await adminFetch("/categories", "abc.123", { fallback: "falhou" });

    const [, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    const headers = (init as RequestInit).headers as Record<string, string>;
    expect(headers["Content-Type"]).toBeUndefined();
  });

  it("givenUmCorpo_whenChama_thenMandaJsonComContentType", async () => {
    await adminFetch("/categories", "abc.123", {
      method: "POST",
      body: { name: "Documentário" },
      fallback: "falhou",
    });

    const [, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    expect((init as RequestInit).method).toBe("POST");
    expect((init as RequestInit).body).toBe(JSON.stringify({ name: "Documentário" }));
    const headers = (init as RequestInit).headers as Record<string, string>;
    expect(headers["Content-Type"]).toBe("application/json");
  });

  it("givenRespostaDeErroComMensagem_whenChama_thenEstouraComElaEOStatus", async () => {
    respondeCom({ message: "Categoria não encontrada" }, { status: 404 });

    const erro = await adminFetch("/categories/1", "abc.123", { fallback: "falhou" }).catch(
      (e: unknown) => e,
    );

    expect(erro).toBeInstanceOf(AdminApiError);
    expect((erro as AdminApiError).status).toBe(404);
    expect((erro as AdminApiError).message).toBe("Categoria não encontrada");
  });

  // O caso real do 403 da admin-api: o Spring Security responde sem corpo nenhum.
  it("givenRespostaDeErroSemCorpo_whenChama_thenUsaAMensagemReserva", async () => {
    respondeCom(undefined, { status: 403 });

    const erro = await adminFetch("/categories", "abc.123", { fallback: "sem permissão" }).catch(
      (e: unknown) => e,
    );

    expect((erro as AdminApiError).status).toBe(403);
    expect((erro as AdminApiError).message).toBe("sem permissão");
  });

  // O `ApiError` da admin-api manda a lista inteira em `errors`; `message` é só a primeira.
  it("givenVariasFalhasDeValidacao_whenChama_thenTodasChegam", async () => {
    respondeCom(
      {
        message: "'name' should not be empty",
        errors: ["'name' should not be empty", "'description' is too long"],
      },
      { status: 422 },
    );

    const erro = (await adminFetch("/categories", "abc.123", {
      method: "POST",
      body: {},
      fallback: "falhou",
    }).catch((e: unknown) => e)) as AdminApiError;

    expect(erro.message).toBe("'name' should not be empty");
    expect(erro.errors).toEqual(["'name' should not be empty", "'description' is too long"]);
  });

  it("givenErroSemListaDeErros_whenChama_thenAListaFicaVazia", async () => {
    respondeCom({ message: "Category with ID 123 was not found" }, { status: 404 });

    const erro = (await adminFetch("/categories/123", "abc.123", { fallback: "falhou" }).catch(
      (e: unknown) => e,
    )) as AdminApiError;

    expect(erro.errors).toEqual([]);
  });

  it("givenMensagemEmBranco_whenChama_thenUsaAMensagemReserva", async () => {
    respondeCom({ message: "   " }, { status: 500 });

    const erro = await adminFetch("/categories", "abc.123", { fallback: "falhou" }).catch(
      (e: unknown) => e,
    );

    expect((erro as AdminApiError).message).toBe("falhou");
  });

  // 204 não tem corpo, e ler JSON de corpo vazio estoura — é a resposta do DELETE da admin-api.
  it("given204_whenChama_thenNaoTentaLerCorpo", async () => {
    respondeCom(undefined, { status: 204 });

    await expect(
      adminFetch("/categories/1", "abc.123", { method: "DELETE", fallback: "falhou" }),
    ).resolves.toBeUndefined();
  });
});
