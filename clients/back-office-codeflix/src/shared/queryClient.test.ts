import { describe, expect, it } from "vitest";
import { AdminApiError } from "@/shared/http";
import { newQueryClient } from "@/shared/queryClient";

function devoRepetir(erro: unknown, tentativasJaFeitas = 0): boolean {
  const retry = newQueryClient().getDefaultOptions().queries?.retry;
  if (typeof retry !== "function") {
    throw new Error("a política de repetição deixou de ser uma função");
  }
  return retry(tentativasJaFeitas, erro as Error) as boolean;
}

describe("política de repetição", () => {
  // O padrão do TanStack é três tentativas. Num 4xx isso vira três chamadas e segundos de espera
  // antes de a pessoa ler "sem permissão" — quando a primeira resposta já disse tudo.
  it("given403_whenFalha_thenNaoRepete", () => {
    expect(devoRepetir(new AdminApiError(403, "Forbidden"))).toBe(false);
  });

  it("given404_whenFalha_thenNaoRepete", () => {
    expect(devoRepetir(new AdminApiError(404, "Not Found"))).toBe(false);
  });

  // 5xx e rede caída são os casos em que tentar de novo pode dar outro resultado.
  it("given500_whenFalha_thenRepete", () => {
    expect(devoRepetir(new AdminApiError(500, "Erro interno"))).toBe(true);
  });

  it("givenFalhaDeRede_whenFalha_thenRepete", () => {
    expect(devoRepetir(new TypeError("fetch failed"))).toBe(true);
  });

  it("givenDuasTentativasJaFeitas_whenFalhaDeNovo_thenDesiste", () => {
    expect(devoRepetir(new TypeError("fetch failed"), 2)).toBe(false);
  });
});
