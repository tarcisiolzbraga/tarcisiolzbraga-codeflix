import type { Account } from "@/account/account";
import { SUBSCRIPTION_API } from "@/shared/config";
import { bearer, messageOf } from "@/shared/http";

// Os quatro estados que o domínio tem, em minúsculo como ele os serializa.
export type SubscriptionStatus = "trailing" | "active" | "incomplete" | "canceled";

export type Subscription = {
  id: string;
  planId: string;
  status: SubscriptionStatus | string;
  dueDate: string;
  lastRenewDate: string | null;
  createdAt: string;
};

// A cobrança responde **200 mesmo quando o cartão é recusado**: a requisição foi atendida, e o que
// aconteceu está no corpo. Tratar isso como erro de HTTP esconderia o motivo da recusa.
export type ChargeOutcome = {
  subscription: Subscription;
  charged: boolean;
  transactionId: string | null;
  reason: string | null;
};

export class SubscriptionRequestFailed extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "SubscriptionRequestFailed";
  }
}

async function call<T>(
  path: string,
  token: string,
  init: { method: string; body?: unknown; fallback: string },
): Promise<T> {
  const response = await fetch(`${SUBSCRIPTION_API}${path}`, {
    method: init.method,
    headers: {
      Accept: "application/json",
      ...bearer(token),
      ...(init.body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: init.body === undefined ? undefined : JSON.stringify(init.body),
  });

  if (!response.ok) {
    throw new SubscriptionRequestFailed(response.status, await messageOf(response, init.fallback));
  }

  return (await response.json()) as T;
}

// Assinar nasce em período de experiência, vencendo em um mês, e **sem cobrança**: cobrar é ato
// separado na API, e o portal não finge o contrário.
export function subscribeTo(planId: string, token: string): Promise<Subscription> {
  return call<Subscription>("/subscriptions", token, {
    method: "POST",
    body: { planId },
    fallback: "Não conseguimos assinar agora.",
  });
}

// **404 aqui não é erro: é "nunca assinou"**, que é o estado normal de quem acabou de criar conta.
// Devolver null em vez de estourar deixa a tela decidir o que mostrar sem inspecionar status HTTP.
export async function fetchMySubscription(token: string): Promise<Subscription | null> {
  const response = await fetch(`${SUBSCRIPTION_API}/subscriptions/me`, {
    headers: { Accept: "application/json", ...bearer(token) },
  });

  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new SubscriptionRequestFailed(
      response.status,
      await messageOf(response, "Não conseguimos carregar a sua assinatura."),
    );
  }
  return (await response.json()) as Subscription;
}

// Cancelar o que já está cancelado não é erro, por contrato.
export function cancelSubscription(token: string): Promise<Subscription> {
  return call<Subscription>("/subscriptions/me/cancel", token, {
    method: "PUT",
    fallback: "Não conseguimos cancelar agora.",
  });
}

// Sem token de cartão, a API usa o meio de pagamento guardado na conta.
export function chargeSubscription(token: string, creditCardToken?: string): Promise<ChargeOutcome> {
  return call<ChargeOutcome>("/subscriptions/me/charge", token, {
    method: "POST",
    body: creditCardToken ? { creditCardToken } : {},
    fallback: "Não conseguimos tentar a cobrança agora.",
  });
}

// O que é guardado é **uma referência do provedor**, não dado de cartão: a API troca o token de uso
// único por uma referência durável e guarda só ela. Nenhum número de cartão passa por aqui — e é
// por isso que esta tela não tem campo de número, validade ou CVV.
export function savePaymentMethod(token: string, creditCardToken: string): Promise<Account> {
  return call<Account>("/accounts/me/payment-method", token, {
    method: "PUT",
    body: { creditCardToken },
    fallback: "Não conseguimos guardar o meio de pagamento.",
  });
}

export function statusLabel(status: string): string {
  switch (status) {
    case "trailing":
      return "em período de experiência";
    case "active":
      return "ativa";
    case "incomplete":
      return "pagamento pendente";
    case "canceled":
      return "cancelada";
    default:
      // Estado novo no domínio aparece como está, em vez de virar "desconhecido" e esconder o que é.
      return status;
  }
}

// O acervo abre para assinatura viva — o que exclui a cancelada. A `videos-api` decide pelo papel no
// token, não por isto; aqui é só o que a tela mostra.
export function isLive(subscription: Subscription): boolean {
  return subscription.status !== "canceled";
}
