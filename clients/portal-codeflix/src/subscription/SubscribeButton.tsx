"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { subscribeTo, SubscriptionRequestFailed } from "@/subscription/subscription";

// O botão de assinar, dentro do cartão do plano. É cliente porque precisa do token de quem está
// logado — e porque é aqui que mora a renovação do token depois de assinar.
export function SubscribeButton({ planId }: { planId: string }) {
  const auth = useAuth();
  const router = useRouter();
  const [working, setWorking] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);

  if (auth.isLoading) {
    return <span className="text-sm text-neutral-500">…</span>;
  }

  if (!auth.isAuthenticated) {
    return (
      <button
        type="button"
        onClick={() => void auth.signinRedirect()}
        className="rounded bg-neutral-800 px-4 py-2 text-sm font-semibold text-neutral-100 hover:bg-neutral-700"
      >
        Entrar para assinar
      </button>
    );
  }

  async function subscribe() {
    const token = auth.user?.access_token;
    if (!token) {
      return;
    }
    setWorking(true);
    setFailure(null);
    try {
      await subscribeTo(planId, token);

      // **A renovação do token não é zelo: sem ela o acervo responde 403.**
      //
      // A assinatura concede CODEFLIX_SUBSCRIBER por evento, alguns segundos depois; o token que
      // está na mão foi assinado antes disso e não muda sozinho. `signinSilent` troca por um novo,
      // já com o papel — e se ele ainda não tiver chegado, a tela da assinatura diz isso em vez de
      // o acervo dar 403 sem explicação.
      await auth.signinSilent();

      router.push("/assinatura");
    } catch (error: unknown) {
      console.error("falha ao assinar", error);
      setFailure(
        error instanceof SubscriptionRequestFailed
          ? error.message
          : "Não conseguimos assinar agora.",
      );
    } finally {
      setWorking(false);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        disabled={working}
        onClick={() => void subscribe()}
        className="rounded bg-red-600 px-4 py-2 text-sm font-semibold text-neutral-50 hover:bg-red-500 disabled:opacity-60"
      >
        {working ? "Assinando…" : "Assinar"}
      </button>
      {failure && <p className="text-xs text-red-400">{failure}</p>}
    </div>
  );
}
