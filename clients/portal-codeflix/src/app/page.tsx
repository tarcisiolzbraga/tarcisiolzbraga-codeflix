import Link from "next/link";

export default function Home() {
  return (
    <section className="flex flex-col items-start gap-6">
      <h1 className="text-4xl font-bold tracking-tight">Filmes e séries, sem compromisso.</h1>
      <p className="max-w-xl text-neutral-400">
        Escolha um plano para começar. O acervo abre assim que a assinatura é confirmada.
      </p>
      <Link
        href="/planos"
        className="rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500"
      >
        Ver os planos
      </Link>
      {/* O acervo e a conta chegam nas fatias seguintes: esta tela existe para o portal ter uma
          entrada de verdade, e não a página do gerador. */}
    </section>
  );
}
