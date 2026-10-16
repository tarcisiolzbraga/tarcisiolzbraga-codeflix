"use client";

import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { type Account, AccountRequestFailed, fetchMyAccount, fullNameOf } from "@/account/account";

// A conta de quem está logado. É cliente, e não componente de servidor, porque a chamada leva o
// token da pessoa — que vive no browser. É também a primeira tela que exercita o CORS da
// subscription-api de verdade.
export default function MyAccount() {
  const auth = useAuth();
  const [account, setAccount] = useState<Account | null>(null);
  const [failure, setFailure] = useState<string | null>(null);

  const token = auth.user?.access_token;

  useEffect(() => {
    if (!token) {
      return;
    }
    let current = true;
    fetchMyAccount(token)
      .then((it) => {
        if (current) {
          setAccount(it);
        }
      })
      .catch((error: unknown) => {
        console.error("falha ao buscar a conta", error);
        if (!current) {
          return;
        }
        // 403 aqui não é erro de rede: é conta que não tem a role de conta, e a pessoa precisa saber
        // disso, não ver "tente de novo".
        setFailure(
          error instanceof AccountRequestFailed && error.status === 403
            ? "Sua conta não tem permissão para esta área."
            : "Não conseguimos carregar sua conta agora.",
        );
      });
    // Desliga a escrita de estado se a tela sair antes da resposta.
    return () => {
      current = false;
    };
  }, [token]);

  if (auth.isLoading) {
    return <p className="text-neutral-400">Carregando…</p>;
  }

  if (!auth.isAuthenticated) {
    return (
      <section className="flex flex-col items-start gap-4">
        <h1 className="text-3xl font-bold tracking-tight">Sua conta</h1>
        <p className="text-neutral-400">Entre para ver os seus dados.</p>
        <button
          type="button"
          onClick={() => void auth.signinRedirect()}
          className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500"
        >
          Entrar
        </button>
      </section>
    );
  }

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Sua conta</h1>
      {failure && <p className="text-neutral-400">{failure}</p>}
      {!failure && !account && <p className="text-neutral-500">Carregando seus dados…</p>}
      {account && (
        <dl className="grid gap-4 sm:grid-cols-2">
          <Field label="Nome" value={fullNameOf(account)} />
          <Field label="E-mail" value={account.email} />
          <Field label="Documento" value={account.documentNumber ?? "não informado"} />
          <Field
            label="Meio de pagamento"
            value={account.hasPaymentMethod ? "cadastrado" : "nenhum cadastrado"}
          />
          <Field
            label="Endereço de cobrança"
            value={
              account.billingAddress
                ? `${account.billingAddress.zipcode}, ${account.billingAddress.number} — ${account.billingAddress.country}`
                : "não informado"
            }
          />
        </dl>
      )}
    </section>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-neutral-800 bg-neutral-900 p-4">
      <dt className="text-xs uppercase tracking-wide text-neutral-500">{label}</dt>
      <dd className="mt-1 text-neutral-100">{value}</dd>
    </div>
  );
}
