import { CatalogScreen } from "@/catalog/CatalogScreen";

export const metadata = {
  title: "Acervo | Codeflix",
  description: "Os títulos disponíveis para quem assina.",
};

export default function Catalog() {
  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Acervo</h1>
      <CatalogScreen />
    </section>
  );
}
