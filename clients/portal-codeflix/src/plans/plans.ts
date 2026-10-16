import { SUBSCRIPTION_API } from "@/shared/config";

// O plano como a rota aberta o devolve. **Sem `active`**: naquela rota todos estão à venda, e a
// própria API deixou o campo de fora por isso.
export type PlanOnSale = {
  id: string;
  name: string;
  description: string;
  currency: string;
  price: number;
};

// O envelope de listagem do monorepo, igual nas duas APIs: `total` é quantos casam com a busca, não
// quantos vieram nesta página.
export type Page<T> = {
  currentPage: number;
  perPage: number;
  total: number;
  items: T[];
};

export class PlansUnavailableError extends Error {
  constructor(readonly status: number) {
    super(`a subscription-api respondeu ${status} ao listar os planos à venda`);
    this.name = "PlansUnavailableError";
  }
}

// Buscado no **servidor**, e não no browser: a rota é aberta, então não há token a carregar. É também
// por isso que esta fatia não exercita CORS — ele vale para as telas que pedem token, que vêm depois.
//
// Busca dinâmica, em tempo de requisição, e o chamador a envolve em Suspense. As outras duas saídas
// que o Next 16 oferece não servem aqui:
//
// - `"use cache"` faria a página ser pré-renderizada no build, e aí **o build passaria a depender da
//   API no ar** — com cacheComponents ligado foi o que aconteceu na primeira versão: `next build`
//   tentou chamar localhost:8084 e quebrou com ECONNREFUSED. Build não pode depender de serviço;
// - `export const instant = false` bloquearia a rota inteira, perdendo a casca pré-renderizada.
//
// O preço disso é uma ida à API por visita. Para um catálogo de poucos planos é barato, e sempre
// fresco: preço que o operador acabou de mudar aparece na próxima visita, não no próximo build.
export async function listPlansOnSale(): Promise<Page<PlanOnSale>> {
  const response = await fetch(`${SUBSCRIPTION_API}/plans/on-sale`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new PlansUnavailableError(response.status);
  }

  return (await response.json()) as Page<PlanOnSale>;
}

// O preço como quem escolhe o lê, com a moeda que a API mandou — e não um "R$" fixo no código, que
// mentiria no dia em que houver plano em outra moeda.
export function formatPrice(plan: PlanOnSale, locale = "pt-BR"): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: plan.currency,
  }).format(plan.price);
}
