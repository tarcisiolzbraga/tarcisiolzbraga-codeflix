import { formatPrice, type PlanOnSale } from "@/plans/plans";

// Um plano na tela de escolha. **Só apresentação**: quem busca é a página e quem dá a ação é a
// lista, o que mantém este componente testável sem rede, sem roteador e sem contexto de login.
//
// A ação entra como `action` em vez de ser importada aqui: quando o botão de assinar passou a morar
// dentro do cartão, os testes dele quebraram pedindo roteador — sinal de que o cartão tinha deixado
// de ser apresentação.
export function PlanCard({ plan, action }: { plan: PlanOnSale; action?: React.ReactNode }) {
  return (
    <article className="flex flex-col gap-3 rounded-lg border border-neutral-800 bg-neutral-900 p-6">
      <h2 className="text-xl font-semibold text-neutral-100">{plan.name}</h2>
      <p className="grow text-sm text-neutral-400">{plan.description}</p>
      <p className="text-2xl font-bold text-neutral-50">
        {formatPrice(plan)}
        <span className="ml-1 text-sm font-normal text-neutral-400">/mês</span>
      </p>
      <p className="text-xs text-neutral-500">Assinatura mensal, cancelável quando quiser.</p>
      {action}
    </article>
  );
}
