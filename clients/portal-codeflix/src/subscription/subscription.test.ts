import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  cancelSubscription,
  chargeSubscription,
  fetchMySubscription,
  isLive,
  savePaymentMethod,
  statusLabel,
  subscribeTo,
  SubscriptionRequestFailed,
  type Subscription,
} from "@/subscription/subscription";

const assinatura: Subscription = {
  id: "22222222-2222-2222-2222-222222222222",
  planId: "11111111-1111-1111-1111-111111111111",
  status: "trailing",
  dueDate: "2026-11-16",
  lastRenewDate: null,
  createdAt: "2026-10-16T12:00:00Z",
};

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

describe("subscribeTo", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenAPlanAndAToken_whenSubscribes_thenPostsThePlanIdWithTheBearer", async () => {
    // Given a API aceitando
    const fetchSpy = respondWith(assinatura, 201);

    // When assina
    const actual = await subscribeTo(assinatura.planId, "um-token");

    // Then manda o plano e o token
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/subscriptions");
    expect(options?.method).toBe("POST");
    expect(new Headers(options?.headers).get("Authorization")).toBe("Bearer um-token");
    expect(JSON.parse(String(options?.body))).toEqual({ planId: assinatura.planId });
    expect(actual.status).toBe("trailing");
  });

  it("givenSomeoneWhoAlreadySubscribed_whenSubscribesAgain_thenCarriesTheApiMessage", async () => {
    // Given a API recusando com 409, como o contrato diz
    respondWith({ message: "there is already a live subscription" }, 409);

    // When assina de novo / Then o status e a mensagem dela chegam
    await expect(subscribeTo(assinatura.planId, "um-token")).rejects.toBeInstanceOf(
      SubscriptionRequestFailed,
    );
    await expect(subscribeTo(assinatura.planId, "um-token")).rejects.toMatchObject({ status: 409 });
  });
});

describe("fetchMySubscription", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenSomeoneWhoNeverSubscribed_whenFetches_thenReturnsNullInsteadOfFailing", async () => {
    // Given a API respondendo 404, que aqui significa "nunca assinou"
    respondWith(undefined, 404);

    // When busca / Then volta null: é estado normal de quem acabou de criar conta, não erro
    await expect(fetchMySubscription("um-token")).resolves.toBeNull();
  });

  it("givenTheApiFailing_whenFetches_thenStillFails", async () => {
    // Given a API em 500
    respondWith(undefined, 500);

    // When busca / Then estoura: 500 não é "nunca assinou"
    await expect(fetchMySubscription("um-token")).rejects.toBeInstanceOf(SubscriptionRequestFailed);
  });
});

describe("chargeSubscription", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenARefusedCard_whenCharges_thenItIsNotAnHttpErrorAndTheReasonComesInTheBody", async () => {
    // Given o provedor recusando o cartão — **a API responde 200**, porque a requisição foi
    // atendida; o que aconteceu está no corpo
    respondWith({ subscription: { ...assinatura, status: "incomplete" }, charged: false, transactionId: null, reason: "insufficient funds" }, 200);

    // When cobra
    const actual = await chargeSubscription("um-token", "tok_recusado");

    // Then não estoura, e o motivo chega para a tela mostrar
    expect(actual.charged).toBe(false);
    expect(actual.reason).toBe("insufficient funds");
    expect(actual.subscription.status).toBe("incomplete");
  });

  it("givenNoCardToken_whenCharges_thenSendsAnEmptyBodySoTheApiUsesTheStoredMethod", async () => {
    // Given a cobrança aceita
    const fetchSpy = respondWith({ subscription: assinatura, charged: true, transactionId: "tx-1", reason: null });

    // When cobra sem token de cartão
    await chargeSubscription("um-token");

    // Then o corpo vai vazio: é assim que a API usa o meio de pagamento guardado
    expect(JSON.parse(String(fetchSpy.mock.calls[0]![1]?.body))).toEqual({});
  });
});

describe("savePaymentMethod", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenAProviderToken_whenSaves_thenPutsItOnTheAccountRoute", async () => {
    // Given a API aceitando
    const fetchSpy = respondWith({ id: "1", email: "a@b.c", firstname: "A", lastname: "B", documentNumber: null, documentType: null, billingAddress: null, hasPaymentMethod: true });

    // When guarda o meio de pagamento
    const actual = await savePaymentMethod("um-token", "tok_visa_4242");

    // Then vai por PUT para a rota da conta, com o token do provedor — e só com ele: nenhum dado de
    // cartão passa pelo portal
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/accounts/me/payment-method");
    expect(options?.method).toBe("PUT");
    expect(JSON.parse(String(options?.body))).toEqual({ creditCardToken: "tok_visa_4242" });
    expect(actual.hasPaymentMethod).toBe(true);
  });
});

describe("cancelSubscription", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("givenALiveSubscription_whenCancels_thenPutsOnTheCancelRoute", async () => {
    // Given a API cancelando
    const fetchSpy = respondWith({ ...assinatura, status: "canceled" });

    // When cancela
    const actual = await cancelSubscription("um-token");

    // Then usa PUT, e a assinatura volta cancelada
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/subscriptions/me/cancel");
    expect(options?.method).toBe("PUT");
    expect(actual.status).toBe("canceled");
  });
});

describe("statusLabel e isLive", () => {
  it("givenTheFourStatusesOfTheDomain_whenLabels_thenEachHasItsOwnText", () => {
    // Given os quatro estados / When rotula / Then cada um tem texto próprio
    const rotulos = ["trailing", "active", "incomplete", "canceled"].map(statusLabel);
    expect(new Set(rotulos).size).toBe(4);
    expect(statusLabel("incomplete")).toBe("pagamento pendente");
  });

  it("givenAStatusTheFrontDoesNotKnow_whenLabels_thenShowsItAsIs", () => {
    // Given um estado novo no domínio, que o portal ainda não conhece
    // When rotula / Then aparece como está, em vez de virar "desconhecido" e esconder o que é
    expect(statusLabel("paused")).toBe("paused");
  });

  it("givenACanceledSubscription_whenAsksIfItIsLive_thenNo", () => {
    // Given uma assinatura cancelada / When pergunta se está viva / Then não
    expect(isLive({ ...assinatura, status: "canceled" })).toBe(false);
    expect(isLive({ ...assinatura, status: "incomplete" })).toBe(true);
  });
});
