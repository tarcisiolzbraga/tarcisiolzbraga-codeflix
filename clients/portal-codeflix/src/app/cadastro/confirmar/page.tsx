import { Suspense } from "react";
import { ConfirmSignUpScreen } from "@/signup/ConfirmSignUpScreen";

export const metadata = {
  title: "Confirmação do cadastro | Codeflix",
};

// A tela que o link do e-mail abre. A `subscription-api` manda este endereço desde antes de o portal
// existir — é o `amqp.confirmation-url` dela —, e é por isso que a rota é em português.
//
// O Suspense não é enfeite: quem lê o `?token=` é `useSearchParams`, e numa rota pré-renderizada o
// Next exige a fronteira para poder servir a casca antes de conhecer a URL.
export default function ConfirmSignUpPage() {
  return (
    <Suspense fallback={<p className="text-neutral-400">Confirmando…</p>}>
      <ConfirmSignUpScreen />
    </Suspense>
  );
}
