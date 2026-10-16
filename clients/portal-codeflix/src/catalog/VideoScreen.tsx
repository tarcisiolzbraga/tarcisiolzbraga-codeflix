"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
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
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">{video.title}</h1>
        <p className="text-sm text-neutral-500">
          {video.yearLaunched} · {formatDuration(video.duration)} · {ratingLabel(video.rating)}
        </p>
      </header>

      <p className="max-w-3xl text-neutral-300">{video.description}</p>

      {/* **Não há player, e não é esquecimento.** O campo `video` do acervo guarda a localização no
          armazenamento, não uma URL que o browser alcance; a única rota que serve o arquivo é a da
          admin-api, e ela exige papel de administração. Servir mídia a assinante precisa de uma rota
          na videos-api que hoje não existe — está escrito no README, com o que ela precisaria
          decidir. Fingir um player aqui esconderia essa lacuna. */}
      <p className="rounded-lg border border-neutral-800 bg-neutral-900 p-4 text-sm text-neutral-400">
        A reprodução ainda não está disponível: o acervo publica os dados do título, mas servir o
        arquivo a quem assina depende de uma rota que a <code>videos-api</code> ainda não tem.
      </p>

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
