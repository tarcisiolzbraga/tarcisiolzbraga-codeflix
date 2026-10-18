"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { Poster } from "@/catalog/Poster";
import { Rail } from "@/catalog/Rail";
import {
  CatalogRefused,
  fetchGenres,
  fetchRails,
  isMissingCatalogRole,
  searchVideos,
  type Rail as RailData,
  type VideoPage,
} from "@/catalog/catalog";

export function CatalogScreen() {
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [term, setTerm] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState<VideoPage | null>(null);
  const [rails, setRails] = useState<RailData[] | null>(null);
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

  // As trilhas são carregadas **uma vez**, e não a cada busca: elas são a vitrine de quem chega sem
  // termo nenhum, e refazê-las ao digitar seria trabalho jogado fora.
  useEffect(() => {
    if (token === undefined || rails !== null) {
      return;
    }
    let atual = true;
    fetchGenres(token)
      .then((genres) => fetchRails(genres, token))
      .then((carregadas) => {
        if (atual) {
          setRails(carregadas);
        }
      })
      .catch((error: unknown) => {
        // Trilha que não carrega não tira o acervo do ar: a busca continua funcionando, e o que
        // aparece é a grade. Lista vazia marca "já tentei", para não repetir a cada render.
        console.error("falha ao carregar as trilhas", error);
        if (atual) {
          setRails([]);
        }
      });
    return () => {
      atual = false;
    };
  }, [token, rails]);

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

      {/* **Trilhas quando ninguém buscou, grade quando buscou.** Quem chega sem termo quer
          descobrir, e trilha por gênero é como se descobre; quem digitou um termo quer o que casa
          com ele, e trilha ali esconderia resultado atrás de rolagem horizontal. */}
      {search === "" && rails !== null && rails.length > 0 && (
        <div className="flex flex-col gap-8">
          {rails.map((rail) => (
            <Rail key={rail.genre.id} rail={rail} />
          ))}
        </div>
      )}

      {page && page.items.length > 0 && (search !== "" || rails?.length === 0) && (
        <>
          <p className="text-sm text-neutral-500">
            {page.meta.total} {page.meta.total === 1 ? "título" : "títulos"}
          </p>
          <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {page.items.map((video) => (
              <li key={video.id}>
                <Poster video={video} />
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
