import { unstable_rethrow } from "next/navigation";
import { PlanCard } from "@/plans/PlanCard";
import { SubscribeButton } from "@/subscription/SubscribeButton";
import { listPlansOnSale, PlansUnavailableError } from "@/plans/plans";

// A parte da tela que depende da API, separada da página de propósito: é ela que fica dentro do
// Suspense, então a casca (o título) chega pré-renderizada e só esta parte espera a resposta.
export async function PlanList() {
  let page;
  try {
    page = await listPlansOnSale();
  } catch (error) {
    // Primeiro devolve o que é do framework, e não da API.
    //
    // `catch` em componente de servidor engole mais do que parece: redirect(), notFound() e a
    // interrupção da pré-renderização viajam como exceção. Sem isto, o build imprimia "falha ao
    // listar os planos" a cada passada de prerender — o Next cancela a busca quando a casca
    // estática termina, e eu estava tratando esse cancelamento como API fora do ar.
    unstable_rethrow(error);

    // Daqui para baixo é erro de verdade: vira recado legível, e não pilha na tela. O motivo vai
    // para o log do servidor, onde serve a quem opera — e não a quem só queria assinar.
    console.error("falha ao listar os planos à venda", error);
    const outOfReach = error instanceof PlansUnavailableError || error instanceof TypeError;
    return (
      <p className="text-neutral-400">
        {outOfReach
          ? "Não conseguimos carregar os planos agora. Tente de novo em instantes."
          : "Algo deu errado ao carregar os planos."}
      </p>
    );
  }

  if (page.items.length === 0) {
    return <p className="text-neutral-400">Nenhum plano à venda neste momento.</p>;
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {page.items.map((plan) => (
        <PlanCard key={plan.id} plan={plan} action={<SubscribeButton planId={plan.id} />} />
      ))}
    </div>
  );
}

// O que aparece enquanto a API responde. Três blocos do tamanho de um cartão, para a tela não pular
// quando os planos chegarem.
export function PlanListSkeleton() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-hidden>
      {[0, 1, 2].map((i) => (
        <div key={i} className="h-48 animate-pulse rounded-lg border border-neutral-800 bg-neutral-900" />
      ))}
    </div>
  );
}
