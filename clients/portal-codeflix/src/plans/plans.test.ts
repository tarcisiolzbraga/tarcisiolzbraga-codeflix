import { beforeEach, describe, expect, it, vi } from "vitest";
import { formatPrice, listPlansOnSale, PlansUnavailableError, type PlanOnSale } from "@/plans/plans";

const basic: PlanOnSale = {
  id: "11111111-1111-1111-1111-111111111111",
  name: "Básico",
  description: "Um aparelho, resolução padrão.",
  currency: "BRL",
  price: 29.9,
};

function respondWith(body: unknown, status = 200) {
  return vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }),
  );
}

describe("listPlansOnSale", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("givenAPageOfPlans_whenLists_thenReturnsTheEnvelopeAndTheItems", async () => {
    // Given a rota aberta devolvendo uma página com um plano
    respondWith({ currentPage: 0, perPage: 20, total: 1, items: [basic] });

    // When lista os planos à venda
    const actual = await listPlansOnSale();

    // Then o envelope e o item chegam como a API os mandou
    expect(actual.total).toBe(1);
    expect(actual.items).toHaveLength(1);
    expect(actual.items[0]?.name).toBe("Básico");
  });

  it("givenTheConfiguredApi_whenLists_thenCallsTheOpenRouteWithoutToken", async () => {
    // Given a API configurada
    const fetchSpy = respondWith({ currentPage: 0, perPage: 20, total: 0, items: [] });

    // When lista
    await listPlansOnSale();

    // Then chama /plans/on-sale e **não** manda Authorization: a rota é aberta de propósito, e
    // mandar token aqui esconderia o dia em que ela deixasse de ser
    const [url, options] = fetchSpy.mock.calls[0]!;
    expect(String(url)).toBe("http://localhost:8084/plans/on-sale");
    expect(new Headers(options?.headers).has("Authorization")).toBe(false);
  });

  it("givenAnApiThatAnswersWithAnError_whenLists_thenFailsCarryingTheStatus", async () => {
    // Given a API respondendo 503
    respondWith({}, 503);

    // When lista / Then estoura dizendo o status, que é o que o log precisa
    await expect(listPlansOnSale()).rejects.toBeInstanceOf(PlansUnavailableError);
    await expect(listPlansOnSale()).rejects.toThrow(/respondeu 503/);
  });
});

describe("formatPrice", () => {
  it("givenAPlanInBrl_whenFormats_thenUsesTheCurrencyTheApiSent", () => {
    // Given um plano em BRL
    // When formata o preço
    const actual = formatPrice(basic);

    // Then sai com o símbolo da moeda e duas casas — o espaço do pt-BR não é o comum, então a
    // asserção é por partes em vez de texto exato
    expect(actual).toContain("R$");
    expect(actual).toContain("29,90");
  });

  it("givenAPlanInAnotherCurrency_whenFormats_thenDoesNotHardcodeTheRealSign", () => {
    // Given um plano em dólar, que o modelo da API permite
    const inUsd: PlanOnSale = { ...basic, currency: "USD", price: 9.99 };

    // When formata
    const actual = formatPrice(inUsd, "en-US");

    // Then não aparece R$ — é a moeda da API que manda
    expect(actual).toBe("$9.99");
  });
});
