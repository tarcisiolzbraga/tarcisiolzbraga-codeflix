"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import {
  CatalogRefused,
  formatDuration,
  isMissingCatalogRole,
  ratingLabel,
  searchVideos,
  type VideoPage,
} from "@/catalog/catalog";

export function CatalogScreen() {
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [term, setTerm] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState<VideoPage | null>(null);
  const [refused, setRefused] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [renewing, setRenewing] = useState(false);

  const load = useCallback(
    (accessToken: string, text: string) => {
      // Nada de estado escrito aqui de forma síncrona: o efeito chama esta função direto, e escrever
      // antes da ida à rede dispara renderização em cascata. O erro anterior é limpo na volta.
      searchVideos({ search: text, page: 0 }, accessToken)
        .then((it) => {
          setPage(it);
          setRefused(false);
          setFailure(null);
        })
        .catch((error: unknown) => {
          console.error("falha ao buscar o acervo", error);
          if (isMissingCatalogRole(error)) {
            setRefused(true);
            return;
          }
          setFailure(
            error instanceof CatalogRefused
              ? "Entre para ver o acervo."
              : "Não conseguimos carregar o acervo agora.",
          );
        });
    },
    [],
  );

  useEffect(() => {
    if (token) {
      load(token, search);
    }
  }, [token, search, load]);

  if (auth.isLoading) {
    return <p className="text-neutral-400">Carregando…</p>;
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p className="text-neutral-400">Entre para ver o acervo.</p>
        <button
          type="button"
          onClick={() => void auth.signinRedirect()}
          className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500"
        >
          Entrar
        </button>
      </div>
    );
  }

  // **A recusa mais comum aqui não é falta de assinatura: é o token velho.**
  //
  // O papel do catálogo é concedido por evento, segundos depois de assinar, e o token em mãos foi
  // assinado antes disso. Dizer "acesso negado" a quem acabou de pagar seria mentir — o certo é
  // oferecer renovar a sessão, que é o que de fato resolve.
  if (refused) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p className="max-w-xl text-neutral-300">
          Seu acesso ao acervo ainda não está no seu token. Se você acabou de assinar, isso leva
          alguns segundos — renove a sessão para continuar.
        </p>
        <button
          type="button"
          disabled={renewing}
          onClick={() => {
            setRenewing(true);
            void auth
              .signinSilent()
              .catch((error: unknown) => console.error("falha ao renovar a sessão", error))
              .finally(() => setRenewing(false));
          }}
          className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500 disabled:opacity-60"
        >
          {renewing ? "Renovando…" : "Renovar a sessão"}
        </button>
        <Link href="/assinatura" className="text-sm text-neutral-400 hover:text-neutral-200">
          Ver a minha assinatura
        </Link>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <form
        className="flex gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          setSearch(term.trim());
        }}
      >
        <input
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          placeholder="Buscar no acervo"
          className="w-full max-w-md rounded border border-neutral-700 bg-neutral-900 px-3 py-2 text-neutral-100"
        />
        <button
          type="submit"
          className="rounded border border-neutral-700 px-4 py-2 text-sm text-neutral-300 hover:bg-neutral-900"
        >
          Buscar
        </button>
      </form>

      {failure && <p className="text-neutral-400">{failure}</p>}

      {page && page.items.length === 0 && (
        <p className="text-neutral-400">
          {search ? `Nada encontrado para "${search}".` : "O acervo está vazio por enquanto."}
        </p>
      )}

      {page && page.items.length > 0 && (
        <>
          <p className="text-sm text-neutral-500">
            {page.meta.total} {page.meta.total === 1 ? "título" : "títulos"}
          </p>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {page.items.map((video) => (
              <li key={video.id}>
                <Link
                  href={`/acervo/${video.id}`}
                  className="flex h-full flex-col gap-2 rounded-lg border border-neutral-800 bg-neutral-900 p-4 hover:border-neutral-700"
                >
                  <h2 className="font-semibold text-neutral-100">{video.title}</h2>
                  <p className="text-xs text-neutral-500">
                    {video.yearLaunched} · {formatDuration(video.duration)} ·{" "}
                    {ratingLabel(video.rating)}
                  </p>
                  <p className="line-clamp-3 text-sm text-neutral-400">{video.description}</p>
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
