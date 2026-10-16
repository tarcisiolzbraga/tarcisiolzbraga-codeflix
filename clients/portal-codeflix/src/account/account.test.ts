import { beforeEach, describe, expect, it, vi } from "vitest";
import { AccountRequestFailed, fetchMyAccount, fullNameOf } from "@/account/account";

const resposta = {
  id: "11111111-1111-1111-1111-111111111111",
  email: "assinante@codeflix.com.br",
  firstname: "Tarcisio",
  lastname: "Braga",
  documentNumber: "12345678901",
  documentType: "CPF",
  billingAddress: { zipcode: "01001000", number: "100", complement: null, country: "BR" },
  hasPaymentMethod: false,
};

function respondWith(body: unknown, status = 200) {
  return vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }),
  );
}

describe("fetchMyAccount", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("givenATokenAndTheApiAnswering_whenFetches_thenReturnsTheAccount", async () => {
    // Given a API devolvendo a conta
    respondWith(resposta);

    // When busca com o token
    const actual = await fetchMyAccount("um-token");

    // Then a conta chega como a API a mandou
    expect(actual.email).toBe("assinante@codeflix.com.br");
    expect(actual.hasPaymentMethod).toBe(false);
  });

  it("givenAToken_whenFetches_thenSendsItAsBearer", async () => {
    // Given a API configurada
    const fetchSpy = respondWith(resposta);

    // When busca
    await fetchMyAccount("um-token");

    // Then o token vai no Authorization. É o oposto do teste da listagem de planos, que afirma que
    // aquela rota **não** leva token: as duas afirmações juntas é que fixam o contrato.
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/accounts/me");
    expect(new Headers(options?.headers).get("Authorization")).toBe("Bearer um-token");
  });

  it("givenATokenWithoutTheAccountRole_whenFetches_thenFailsCarryingThe403", async () => {
    // Given a API recusando por falta de papel
    respondWith({}, 403);

    // When busca / Then o status vem no erro, porque 403 e 500 pedem recados diferentes na tela
    await expect(fetchMyAccount("um-token")).rejects.toBeInstanceOf(AccountRequestFailed);
    await expect(fetchMyAccount("um-token")).rejects.toMatchObject({ status: 403 });
  });

  it("givenNoToken_whenTheApiRefuses_thenFailsWith401", async () => {
    // Given a API recusando sem token
    respondWith({}, 401);

    // When busca / Then o 401 chega no erro
    await expect(fetchMyAccount("")).rejects.toMatchObject({ status: 401 });
  });
});

describe("fullNameOf", () => {
  it("givenFirstAndLastName_whenJoins_thenReturnsBoth", () => {
    // Given nome e sobrenome
    // When junta / Then vem o nome inteiro
    expect(fullNameOf({ firstname: "Tarcisio", lastname: "Braga" })).toBe("Tarcisio Braga");
  });

  it("givenOnlyTheFirstName_whenJoins_thenDoesNotLeaveATrailingSpace", () => {
    // Given só o primeiro nome, que é o que o token traz quando o sobrenome não foi preenchido
    // When junta / Then não sobra espaço no fim, senão o cabeçalho mostra "Tarcisio " com buraco
    expect(fullNameOf({ firstname: "Tarcisio", lastname: "" })).toBe("Tarcisio");
  });
});
