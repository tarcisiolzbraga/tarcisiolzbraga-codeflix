import { describe, expect, it } from "vitest";
import { AdminApiError } from "@/shared/http";
import { explainFailure } from "@/shared/explainFailure";

describe("explainFailure", () => {
  // Os dois que parecem o mesmo na tela e são opostos.
  it("given401_whenExplicado_thenFalaDaSessao", () => {
    expect(explainFailure(new AdminApiError(401, "Unauthorized"))).toBe(
      "Sua sessão expirou. Entre de novo.",
    );
  });

  it("given403_whenExplicado_thenFalaDePermissao", () => {
    expect(explainFailure(new AdminApiError(403, "Forbidden"))).toBe(
      "Sua conta não tem permissão para esta operação.",
    );
  });

  // A admin-api diz o que está errado na validação; repetir a mensagem dela é melhor que inventar.
  it("givenErroComMensagemDaApi_whenExplicado_thenRepeteAMensagem", () => {
    expect(explainFailure(new AdminApiError(422, "Parâmetro 'dir' inválido"))).toBe(
      "Parâmetro 'dir' inválido",
    );
  });

  it("given500_whenExplicado_thenRepeteAMensagem", () => {
    expect(explainFailure(new AdminApiError(500, "Erro interno"))).toBe("Erro interno");
  });

  // `fetch` rejeita com TypeError quando não há ninguém escutando na porta.
  it("givenFalhaDeRede_whenExplicada_thenApontaParaAApiForaDoAr", () => {
    expect(explainFailure(new TypeError("fetch failed"))).toBe(
      "Não foi possível falar com a admin-api. Verifique se ela está no ar.",
    );
  });

  it("givenCoisaQueNemEhErro_whenExplicada_thenNaoEstoura", () => {
    expect(explainFailure("alguma coisa")).toContain("admin-api");
  });
});
