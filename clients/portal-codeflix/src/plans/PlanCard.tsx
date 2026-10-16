import { formatPrice, type PlanOnSale } from "@/plans/plans";

// Um plano na tela de escolha. Só apresentação: quem busca é a página, o que mantém este componente
// testável sem rede.
export function PlanCard({ plan }: { plan: PlanOnSale }) {
  return (
    <article className="flex flex-col gap-3 rounded-lg border border-neutral-800 bg-neutral-900 p-6">
      <h2 className="text-xl font-semibold text-neutral-100">{plan.name}</h2>
      <p className="grow text-sm text-neutral-400">{plan.description}</p>
      <p className="text-2xl font-bold text-neutral-50">
        {formatPrice(plan)}
        <span className="ml-1 text-sm font-normal text-neutral-400">/mês</span>
      </p>
      {/* Sem ação ainda: assinar exige conta, e a conta exige o login, que é a fatia seguinte. */}
      <p className="text-xs text-neutral-500">Assinatura mensal, cancelável quando quiser.</p>
    </article>
  );
}
