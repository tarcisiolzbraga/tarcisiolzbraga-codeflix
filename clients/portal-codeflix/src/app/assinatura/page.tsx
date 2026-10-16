"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import {
  cancelSubscription,
  chargeSubscription,
  fetchMySubscription,
  savePaymentMethod,
  statusLabel,
  type ChargeOutcome,
  type Subscription,
  SubscriptionRequestFailed,
} from "@/subscription/subscription";

export default function MySubscription() {
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<ChargeOutcome | null>(null);
  const [card, setCard] = useState("");
  const [working, setWorking] = useState(false);

  const reportFailure = useCallback((error: unknown, fallback: string) => {
    console.error(fallback, error);
    setFailure(error instanceof SubscriptionRequestFailed ? error.message : fallback);
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }
    let current = true;
    fetchMySubscription(token)
      .then((it) => {
        if (current) {
          setSubscription(it);
          setLoaded(true);
        }
      })
      .catch((error: unknown) => {
        if (current) {
          reportFailure(error, "Não conseguimos carregar a sua assinatura.");
          setLoaded(true);
        }
      });
    return () => {
      current = false;
    };
  }, [token, reportFailure]);

  async function act(what: () => Promise<void>, fallback: string) {
    setWorking(true);
    setFailure(null);
    try {
      await what();
    } catch (error: unknown) {
      reportFailure(error, fallback);
    } finally {
      setWorking(false);
    }
  }

  if (auth.isLoading) {
    return <p className="text-neutral-400">Carregando…</p>;
  }

  if (!auth.isAuthenticated) {
    return (
      <Screen>
        <p className="text-neutral-400">Entre para ver a sua assinatura.</p>
        <button
          type="button"
          onClick={() => void auth.signinRedirect()}
          className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500"
        >
          Entrar
        </button>
      </Screen>
    );
  }

  // Autenticado e ainda sem token em mãos é estado real, por um instante, entre a volta do Keycloak
  // e a leitura do armazenamento. Tratar aqui também faz o TypeScript saber que daqui para baixo o
  // token existe — sem precisar de asserção de não-nulo em cada uso.
  if (!token) {
    return (
      <Screen>
        <p className="text-neutral-500">Carregando a sua sessão…</p>
      </Screen>
    );
  }

  if (!loaded) {
    return (
      <Screen>
        <p className="text-neutral-500">Carregando a sua assinatura…</p>
      </Screen>
    );
  }

  // Nunca ter assinado é estado normal de quem acabou de criar conta, e não erro: a API responde 404
  // e o portal mostra o caminho em vez de um aviso vermelho.
  if (!subscription) {
    return (
      <Screen>
        <p className="text-neutral-400">Você ainda não tem assinatura.</p>
        <Link
          href="/planos"
          className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500"
        >
          Escolher um plano
        </Link>
      </Screen>
    );
  }

  return (
    <Screen>
      {failure && <p className="text-red-400">{failure}</p>}

      <dl className="grid w-full gap-4 sm:grid-cols-2">
        <Field label="Situação" value={statusLabel(subscription.status)} />
        <Field label="Vence em" value={new Date(subscription.dueDate).toLocaleDateString("pt-BR")} />
      </dl>

      {outcome && (
        <p className={outcome.charged ? "text-neutral-300" : "text-red-400"}>
          {outcome.charged
            ? `Pagamento aprovado${outcome.transactionId ? ` (${outcome.transactionId})` : ""}.`
            : `O pagamento não passou${outcome.reason ? `: ${outcome.reason}` : "."}`}
        </p>
      )}

      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          disabled={working}
          onClick={() =>
            void act(async () => {
              const result = await chargeSubscription(token, card.trim() || undefined);
              setOutcome(result);
              setSubscription(result.subscription);
            }, "Não conseguimos tentar a cobrança agora.")
          }
          className="rounded bg-red-600 px-4 py-2 text-sm font-semibold text-neutral-50 hover:bg-red-500 disabled:opacity-60"
        >
          Pagar agora
        </button>

        {subscription.status !== "canceled" && (
          <button
            type="button"
            disabled={working}
            onClick={() =>
              void act(async () => {
                setSubscription(await cancelSubscription(token));
              }, "Não conseguimos cancelar agora.")
            }
            className="rounded border border-neutral-700 px-4 py-2 text-sm text-neutral-300 hover:bg-neutral-900 disabled:opacity-60"
          >
            Cancelar assinatura
          </button>
        )}
      </div>

      <form
        className="flex w-full max-w-xl flex-col gap-2 rounded-lg border border-neutral-800 bg-neutral-900 p-4"
        onSubmit={(event) => {
          event.preventDefault();
          void act(async () => {
            await savePaymentMethod(token, card.trim());
            setCard("");
          }, "Não conseguimos guardar o meio de pagamento.");
        }}
      >
        <label className="flex flex-col gap-1">
          <span className="text-xs uppercase tracking-wide text-neutral-500">
            Token do provedor de pagamento
          </span>
          <input
            value={card}
            onChange={(event) => setCard(event.target.value)}
            placeholder="tok_visa_4242"
            className="rounded border border-neutral-700 bg-neutral-950 px-3 py-2 text-neutral-100"
          />
        </label>
        {/* **Não há campo de número, validade ou CVV, e isso é de propósito.** Num provedor de
            verdade, quem recebe o cartão é o SDK dele no browser, que devolve um token de uso único;
            a API troca esse token por uma referência durável e guarda só a referência. Dado de
            cartão não passa nem pelo portal nem pelo serviço. */}
        <p className="text-xs text-neutral-500">
          Num provedor de verdade este valor vem do SDK dele, não digitado: dado de cartão não passa
          por aqui. Guardar substitui o meio anterior.
        </p>
        <button
          type="submit"
          disabled={working || !card.trim()}
          className="self-start rounded border border-neutral-700 px-4 py-2 text-sm text-neutral-300 hover:bg-neutral-800 disabled:opacity-60"
        >
          Guardar meio de pagamento
        </button>
      </form>
    </Screen>
  );
}

function Screen({ children }: { children: React.ReactNode }) {
  return (
    <section className="flex flex-col items-start gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Sua assinatura</h1>
      {children}
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
