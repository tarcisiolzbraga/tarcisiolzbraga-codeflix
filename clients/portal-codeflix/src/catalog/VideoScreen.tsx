"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { Artwork } from "@/catalog/Artwork";
import { Player } from "@/catalog/Player";
import {
  fetchVideo,
  formatDuration,
  isMissingCatalogRole,
  ratingLabel,
  type Video,
} from "@/catalog/catalog";

type State =
  | { step: "loading" }
  | { step: "found"; video: Video }
  | { step: "absent" }
  | { step: "refused" }
  | { step: "failed" };

export function VideoScreen({ id }: { id: string }) {
  const auth = useAuth();
  const token = auth.user?.access_token;
  const [state, setState] = useState<State>({ step: "loading" });

  useEffect(() => {
    if (!token) {
      return;
    }
    let current = true;
    fetchVideo(id, token)
      .then((video) => {
        if (current) {
          setState(video ? { step: "found", video } : { step: "absent" });
        }
      })
      .catch((error: unknown) => {
        console.error("falha ao buscar o título", error);
        if (current) {
          setState(isMissingCatalogRole(error) ? { step: "refused" } : { step: "failed" });
        }
      });
    return () => {
      current = false;
    };
  }, [id, token]);

  if (auth.isLoading || state.step === "loading") {
    return <p className="text-neutral-400">Carregando…</p>;
  }

  if (!auth.isAuthenticated || state.step === "refused") {
    return (
      <div className="flex flex-col items-start gap-3">
        <p className="text-neutral-400">Este título é para quem assina.</p>
        <Link href="/acervo" className="text-sm text-neutral-300 hover:text-neutral-50">
          Voltar ao acervo
        </Link>
      </div>
    );
  }

  // Nulo é resposta normal do schema: "o vídeo não é servido aqui". Um id válido de algo que não
  // está publicado cai neste caso, e não em erro.
  if (state.step === "absent") {
    return (
      <div className="flex flex-col items-start gap-3">
        <p className="text-neutral-400">Este título não está no acervo.</p>
        <Link href="/acervo" className="text-sm text-neutral-300 hover:text-neutral-50">
          Voltar ao acervo
        </Link>
      </div>
    );
  }

  if (state.step === "failed") {
    return <p className="text-neutral-400">Não conseguimos carregar este título agora.</p>;
  }

  const { video } = state;
  return (
    <article className="flex flex-col gap-6">
      {/* O banner é deitado, 16:6, que é a proporção que o `BANNER` do admin recebe. O título vem
          **sobre** ele, com um degradê por baixo: sem o degradê, título claro sobre cena clara fica
          ilegível, e isso depende da arte — que não se controla daqui. */}
      <header className="relative overflow-hidden rounded-lg">
        <Artwork
          videoId={video.id}
          type="BANNER"
          location={video.banner}
          title={video.title}
          className="aspect-16/6 w-full"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-neutral-950/50 to-transparent" />
        <div className="absolute bottom-0 flex flex-col gap-1 p-4 sm:p-6">
          <h1 className="text-3xl font-bold tracking-tight drop-shadow-lg sm:text-4xl">
            {video.title}
          </h1>
          <p className="text-sm text-neutral-300">
            {video.yearLaunched} · {formatDuration(video.duration)} · {ratingLabel(video.rating)}
          </p>
        </div>
      </header>

      <p className="max-w-3xl text-neutral-300">{video.description}</p>

      {/* O player veio depois do resto desta tela: a rota de mídia do catálogo não existia, e a
          página dizia isso em vez de fingir. Hoje existe, com endereço assinado e `Range` — a
          reprodução é progressiva e a busca na linha de tempo funciona. */}
      {/* **O botão só aparece quando há arquivo.** A consulta já diz se o título tem vídeo e
          trailer, e um botão que sempre responde "ainda não tem arquivo" é pior que nenhum — a
          maioria dos títulos recém-cadastrados no back-office não tem nenhum dos cinco.

          O estado de ausência do próprio player continua existindo, para a corrida: o arquivo pode
          sair do ar entre a consulta e o clique. */}
      {video.video === null && video.trailer === null ? (
        <p className="text-sm text-neutral-500">
          Este título ainda não tem arquivo para reproduzir.
        </p>
      ) : (
        <div className="flex flex-col gap-3">
          {video.video !== null && <Player videoId={video.id} />}
          {video.trailer !== null && (
            <Player videoId={video.id} type="TRAILER" label="Ver o trailer" />
          )}
        </div>
      )}

      <Facts label="Gêneros" values={video.genres.map((it) => it.name)} />
      <Facts label="Categorias" values={video.categories.map((it) => it.name)} />
      <Facts label="Elenco" values={video.castMembers.map((it) => it.name)} />

      <Link href="/acervo" className="text-sm text-neutral-400 hover:text-neutral-200">
        Voltar ao acervo
      </Link>
    </article>
  );
}

function Facts({ label, values }: { label: string; values: string[] }) {
  if (values.length === 0) {
    return null;
  }
  return (
    <section className="flex flex-col gap-1">
      <h2 className="text-xs uppercase tracking-wide text-neutral-500">{label}</h2>
      <p className="text-neutral-300">{values.join(", ")}</p>
    </section>
  );
}
