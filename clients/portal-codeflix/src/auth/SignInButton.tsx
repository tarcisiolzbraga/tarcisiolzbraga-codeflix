"use client";

import Link from "next/link";
import { useAuth } from "react-oidc-context";
import { fullNameOf } from "@/account/account";

// Entrar, sair, e quem está logado. Mora no cabeçalho, então é o único lugar do portal que mostra
// estado de autenticação em toda página.
//
// O nome vem do **token**, não de `/accounts/me`: o cabeçalho não deve esperar uma chamada de rede
// para dizer quem está logado. `profile` traz given_name e family_name porque o escopo pedido inclui
// `profile`.
export function SignInButton() {
  const auth = useAuth();

  if (auth.isLoading) {
    return <span className="text-sm text-neutral-500">…</span>;
  }

  if (auth.error) {
    // O erro vai para a tela em uma linha, e não some num console que ninguém abre.
    return <span className="text-sm text-red-500">Falha ao autenticar</span>;
  }

  if (!auth.isAuthenticated) {
    return (
      <button
        type="button"
        onClick={() => void auth.signinRedirect()}
        className="rounded bg-red-600 px-4 py-2 text-sm font-semibold text-neutral-50 hover:bg-red-500"
      >
        Entrar
      </button>
    );
  }

  const profile = auth.user?.profile;
  const name =
    fullNameOf({
      firstname: String(profile?.given_name ?? ""),
      lastname: String(profile?.family_name ?? ""),
    }) || String(profile?.email ?? "sua conta");

  return (
    <span className="flex items-center gap-3 text-sm">
      <Link href="/conta" className="text-neutral-300 hover:text-neutral-50">
        {name}
      </Link>
      <button
        type="button"
        onClick={() => void auth.signoutRedirect()}
        className="text-neutral-500 hover:text-neutral-300"
      >
        Sair
      </button>
    </span>
  );
}
