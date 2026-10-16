import { SUBSCRIPTION_API } from "@/shared/config";
import { bearer } from "@/shared/http";

// A conta como `GET /accounts/me` a devolve.
export type BillingAddress = {
  zipcode: string;
  number: string;
  complement: string | null;
  country: string;
};

export type Account = {
  id: string;
  email: string;
  firstname: string;
  lastname: string;
  documentNumber: string | null;
  documentType: string | null;
  billingAddress: BillingAddress | null;
  // Referência opaca do provedor: o portal só precisa saber **se** existe, nunca o que é.
  hasPaymentMethod: boolean;
};

export class AccountRequestFailed extends Error {
  constructor(readonly status: number) {
    super(`a subscription-api respondeu ${status} em /accounts/me`);
    this.name = "AccountRequestFailed";
  }
}

// Diferente da listagem de planos, esta busca acontece **no browser**, com o token da pessoa — e é
// aqui que o CORS das três APIs finalmente entra em uso. Buscar no servidor exigiria mandar o token
// para o servidor do Next, que é outra decisão e outra superfície.
export async function fetchMyAccount(accessToken: string): Promise<Account> {
  const response = await fetch(`${SUBSCRIPTION_API}/accounts/me`, {
    headers: { Accept: "application/json", ...bearer(accessToken) },
  });

  if (!response.ok) {
    throw new AccountRequestFailed(response.status);
  }

  return (await response.json()) as Account;
}

// Pede só os dois campos que usa, e não a Account inteira: assim serve tanto para a resposta da API
// quanto para o que vem no token, sem ninguém precisar inventar uma Account de mentira.
export function fullNameOf(person: { firstname: string; lastname: string }): string {
  return `${person.firstname} ${person.lastname}`.trim();
}
