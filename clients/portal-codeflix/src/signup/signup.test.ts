import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  confirmSignUp,
  problemsIn,
  requestSignUp,
  SignUpRefused,
  type SignUpForm,
} from "@/signup/signup";

const form: SignUpForm = {
  firstname: "Tarcisio",
  lastname: "Braga",
  email: "assinante@codeflix.com.br",
  password: "uma-senha-boa",
  documentNumber: "11144477735",
  documentType: "cpf",
};

// Uma resposta **nova a cada chamada**, e não a mesma instância: o corpo de um Response só pode ser
// lido uma vez, e os casos que chamam duas vezes (um para o tipo do erro, outro para a mensagem)
// liam o corpo já consumido — o erro virava o texto de reserva, e o teste acusava a coisa errada.
function respondWith(body: unknown, status = 200) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    Promise.resolve(
      new Response(body === undefined ? null : JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" },
      }),
    ),
  );
}

describe("requestSignUp", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("givenAFilledForm_whenRequests_thenPostsToTheOpenRouteWithoutToken", async () => {
    // Given a API aceitando o pedido
    const fetchSpy = respondWith({ message: "a validation email is on its way" }, 202);

    // When pede o cadastro
    await requestSignUp(form);

    // Then vai para a rota aberta, sem Authorization: quem se cadastra ainda não tem token, e é por
    // isso que a rota não pede um
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/accounts/sign-up");
    expect(options?.method).toBe("POST");
    expect(new Headers(options?.headers).has("Authorization")).toBe(false);
    expect(JSON.parse(String(options?.body))).toMatchObject({ documentType: "cpf" });
  });

  it("givenTheApiRefusingWithAMessage_whenRequests_thenCarriesThatMessage", async () => {
    // Given a API recusando com o motivo escrito por ela
    respondWith({ message: "'documentNumber' is not a valid cpf" }, 422);

    // When pede o cadastro / Then a mensagem da API chega, em vez de uma redação inventada aqui
    await expect(requestSignUp(form)).rejects.toBeInstanceOf(SignUpRefused);
    await expect(requestSignUp(form)).rejects.toThrow(/is not a valid cpf/);
  });

  it("givenTheApiRefusingWithoutABody_whenRequests_thenStillSaysSomethingUseful", async () => {
    // Given a API caindo sem corpo, como em 502 de gateway
    respondWith(undefined, 502);

    // When pede o cadastro / Then não estoura ao tentar ler JSON, e sobra um recado legível
    await expect(requestSignUp(form)).rejects.toThrow(/Não conseguimos pedir o seu cadastro/);
  });
});

describe("confirmSignUp", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("givenTheTokenFromTheEmail_whenConfirms_thenSendsItInTheBodyAndGetsTheAccountId", async () => {
    // Given a API confirmando
    const fetchSpy = respondWith({ accountId: "11111111-1111-1111-1111-111111111111" });

    // When confirma com o token
    const actual = await confirmSignUp("um-token");

    // Then o token vai **no corpo**, nunca na URL: a API o recebe assim para ele não cair no log do
    // servidor, e mandá-lo na query string jogaria essa decisão fora
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/accounts/sign-up/confirm");
    expect(String(url)).not.toContain("um-token");
    expect(JSON.parse(String(options?.body))).toEqual({ token: "um-token" });
    expect(actual).toBe("11111111-1111-1111-1111-111111111111");
  });

  it("givenATokenThatDoesNotExist_whenConfirms_thenSaysTheLinkIsNotValid", async () => {
    // Given a API respondendo 404, sem mensagem
    respondWith(undefined, 404);

    // When confirma / Then o recado é sobre o link, e não um "erro inesperado"
    await expect(confirmSignUp("um-token")).rejects.toThrow(/não existe ou não vale mais/);
  });
});

describe("problemsIn", () => {
  it("givenAFilledForm_whenChecked_thenFindsNoProblem", () => {
    // Given o formulário preenchido
    // When confere / Then não há problema
    expect(problemsIn(form)).toEqual([]);
  });

  it("givenAShortPassword_whenChecked_thenSaysTheMinimum", () => {
    // Given senha de menos de 8 caracteres, que é o mínimo do domínio
    // When confere / Then o recado diz o número
    expect(problemsIn({ ...form, password: "curta" })).toEqual([
      "A senha precisa de pelo menos 8 caracteres.",
    ]);
  });

  it("givenSeveralEmptyFields_whenChecked_thenReturnsAllOfThem", () => {
    // Given vários campos vazios
    const empty = { ...form, firstname: "", lastname: "", documentNumber: "" };

    // When confere / Then devolve todos, para a pessoa corrigir de uma vez em vez de um por vez
    expect(problemsIn(empty)).toHaveLength(3);
  });

  it("givenAnInvalidDocument_whenChecked_thenLetsTheApiDecide", () => {
    // Given um documento com dígitos verificadores errados
    const invalid = { ...form, documentNumber: "11111111111" };

    // When confere no portal / Then ele **não** reclama: a conta dos dígitos é da API, e repeti-la
    // aqui seria uma segunda fonte de verdade para a mesma regra
    expect(problemsIn(invalid)).toEqual([]);
  });
});
