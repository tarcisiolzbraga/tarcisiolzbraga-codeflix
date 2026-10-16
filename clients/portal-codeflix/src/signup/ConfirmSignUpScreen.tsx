"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { confirmSignUp, SignUpRefused } from "@/signup/signup";

type State = { step: "checking" } | { step: "done" } | { step: "failed"; message: string };

export function ConfirmSignUpScreen() {
  const params = useSearchParams();

  // O token é capturado na **primeira** renderização, porque logo depois a URL é limpa e o parâmetro
  // desaparece. Sem isto a tela voltaria para "falta o link" justamente depois de confirmar.
  const [token] = useState(() => params.get("token"));
  const [state, setState] = useState<State>({ step: "checking" });

  // Com Strict Mode, o efeito roda duas vezes em desenvolvimento. A confirmação é idempotente por
  // contrato — clicar no link duas vezes é comum —, mas uma chamada só é o certo.
  const asked = useRef(false);

  useEffect(() => {
    if (!token || asked.current) {
      return;
    }
    asked.current = true;

    // Tira o token da URL assim que ele é lido. A API o recebe no corpo justamente para ele não ficar
    // em log de servidor; deixá-lo na barra de endereços o deixaria no histórico do navegador e em
    // qualquer `Referer` que a página mandasse depois.
    window.history.replaceState({}, document.title, window.location.pathname);

    confirmSignUp(token)
      .then(() => setState({ step: "done" }))
      .catch((error: unknown) => {
        console.error("falha ao confirmar o cadastro", error);
        setState({
          step: "failed",
          message:
            error instanceof SignUpRefused ? error.message : "Não conseguimos confirmar o seu cadastro.",
        });
      });
  }, [token]);

  // Sem token, a resposta é de renderização e não de estado: assim nada é escrito dentro do efeito de
  // forma síncrona, que dispararia renderização em cascata.
  if (!token) {
    return (
      <Screen>
        <p className="text-neutral-400">
          Este endereço precisa do link que foi enviado por e-mail. Abra o link da mensagem.
        </p>
      </Screen>
    );
  }

  return (
    <Screen>
      {state.step === "checking" && <p className="text-neutral-400">Confirmando…</p>}
      {state.step === "failed" && <p className="text-neutral-400">{state.message}</p>}
      {state.step === "done" && (
        <>
          <p className="text-neutral-300">Conta criada. Agora entre para escolher o seu plano.</p>
          <Link
            href="/planos"
            className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500"
          >
            Ver os planos
          </Link>
        </>
      )}
    </Screen>
  );
}

function Screen({ children }: { children: React.ReactNode }) {
  return (
    <section className="flex flex-col items-start gap-4">
      <h1 className="text-3xl font-bold tracking-tight">Confirmação do cadastro</h1>
      {children}
    </section>
  );
}
