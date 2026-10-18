"use client";

import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { fetchSignedMediaUrl, type MediaType } from "@/catalog/catalog";

type State =
  | { step: "idle" }
  | { step: "asking" }
  | { step: "ready"; url: string }
  | { step: "absent" }
  | { step: "failed" };

// O player do título, e também o do trailer: os dois são arquivo de áudio e vídeo pela mesma rota, e
// o que muda é o tipo de mídia e o rótulo do botão.
//
// **O trailer não tem regra de acesso própria**, e isto não é descuido: o `MediaAccess` do catálogo
// decide **por vídeo** — pelo `opened` do título e pelo papel de quem pede —, não por tipo de mídia.
// Inventar aqui um trailer aberto a todos seria o front prometendo o que a API recusa.
//
// O `src` é um endereço **assinado** que o catálogo emite: `<video src>` não carrega cabeçalho de
// `Authorization`, então a URL precisa se autorizar sozinha. Com isso a reprodução é progressiva — o
// browser busca o que precisa, quando precisa — e não há download prévio do arquivo inteiro.
//
// O endereço é pedido **no clique**, e não ao abrir a página: a assinatura vale poucos minutos, e
// pedi-la antes faria ela vencer na mão de quem leu a sinopse com calma.
export function Player({
  videoId,
  type = "VIDEO",
  label = "Assistir",
}: {
  videoId: string;
  type?: MediaType;
  label?: string;
}) {
  const auth = useAuth();
  const [state, setState] = useState<State>({ step: "idle" });

  async function play() {
    const token = auth.user?.access_token;
    if (!token) {
      return;
    }
    setState({ step: "asking" });
    try {
      const media = await fetchSignedMediaUrl(videoId, type, token);
      setState(media ? { step: "ready", url: media.url } : { step: "absent" });
    } catch (error: unknown) {
      console.error("falha ao pedir o endereço da mídia", error);
      setState({ step: "failed" });
    }
  }

  if (state.step === "ready") {
    // Sem <track> porque o acervo não publica legenda — o documento do catálogo não tem esse campo.
    // Quando publicar, entra aqui.
    return (
      <video
        controls
        autoPlay
        src={state.url}
        className="w-full rounded-lg border border-neutral-800"
      />
    );
  }

  return (
    <div className="flex flex-col items-start gap-2 rounded-lg border border-neutral-800 bg-neutral-900 p-4">
      {state.step === "absent" && (
        <p className="text-sm text-neutral-400">
          Este título ainda não tem {type === "TRAILER" ? "trailer" : "arquivo"} para reproduzir.
        </p>
      )}
      {state.step === "failed" && (
        <p className="text-sm text-neutral-400">Não conseguimos liberar o vídeo agora.</p>
      )}
      {state.step !== "absent" && (
        <button
          type="button"
          disabled={state.step === "asking"}
          onClick={() => void play()}
          className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500 disabled:opacity-60"
        >
          {state.step === "asking" ? "Liberando…" : label}
        </button>
      )}
    </div>
  );
}
