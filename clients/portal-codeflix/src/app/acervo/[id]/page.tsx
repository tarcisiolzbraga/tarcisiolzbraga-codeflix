import { Suspense } from "react";
import { VideoScreen } from "@/catalog/VideoScreen";

// Ler `params` é acesso dinâmico, e com cacheComponents ligado o Next exige fronteira de Suspense
// para poder servir a casca antes de conhecer a URL. É a mesma regra que a listagem de planos
// encontrou — lá por buscar na API, aqui por depender do parâmetro da rota.
export default function VideoPage({ params }: PageProps<"/acervo/[id]">) {
  return (
    <Suspense fallback={<p className="text-neutral-400">Carregando…</p>}>
      <FromParams params={params} />
    </Suspense>
  );
}

async function FromParams({ params }: { params: PageProps<"/acervo/[id]">["params"] }) {
  const { id } = await params;
  return <VideoScreen id={id} />;
}
