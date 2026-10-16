import { Suspense } from "react";
import { PlanList, PlanListSkeleton } from "@/plans/PlanList";

export const metadata = {
  title: "Planos | Codeflix",
  description: "Os planos à venda, com o preço de cada um.",
};

// A tela de escolha. O título é estático e vem pré-renderizado; a lista chega em tempo de
// requisição, dentro do Suspense — é o que mantém o `next build` independente da API no ar.
export default function Plans() {
  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Escolha o seu plano</h1>
      <Suspense fallback={<PlanListSkeleton />}>
        <PlanList />
      </Suspense>
    </section>
  );
}
