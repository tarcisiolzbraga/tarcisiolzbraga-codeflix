"use client";

import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { fetchSignedMediaUrl } from "@/catalog/catalog";

type State =
  | { step: "idle" }
  | { step: "asking" }
  | { step: "ready"; url: string }
  | { step: "absent" }
  | { step: "failed" };

// O player do título.
//
// O `src` é um endereço **assinado** que o catálogo emite: `<video src>` não carrega cabeçalho de
// `Authorization`, então a URL precisa se autorizar sozinha. Com isso a reprodução é progressiva — o
// browser busca o que precisa, quando precisa — e não há download prévio do arquivo inteiro.
//
// O endereço é pedido **no clique**, e não ao abrir a página: a assinatura vale poucos minutos, e
// pedi-la antes faria ela vencer na mão de quem leu a sinopse com calma.
export function Player({ videoId }: { videoId: string }) {
  const auth = useAuth();
  const [state, setState] = useState<State>({ step: "idle" });

  async function play() {
    const token = auth.user?.access_token;
    if (!token) {
      return;
    }
    setState({ step: "asking" });
    try {
      const media = await fetchSignedMediaUrl(videoId, "VIDEO", token);
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
        <p className="text-sm text-neutral-400">Este título ainda não tem arquivo para reproduzir.</p>
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
          {state.step === "asking" ? "Liberando…" : "Assistir"}
        </button>
      )}
    </div>
  );
}
