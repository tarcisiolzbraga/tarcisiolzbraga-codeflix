import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderAtRoute } from "@/shared/testing";

vi.mock("react-oidc-context", () => ({
  useAuth: () => ({
    user: { profile: { name: "Operador" }, access_token: "abc.123" },
    signoutRedirect: vi.fn(),
  }),
}));

const { AppRoutes } = await import("@/AppRoutes");

// Aqui a tabela é a **do aplicativo**. O teste da tela de "não existe" monta a sua própria rota `*`
// e por isso continuava verde com a rota apagada do roteador: provava o componente, não o caminho
// até ele.
describe("AppRoutes", () => {
  it("givenUmEnderecoQueNaoExiste_whenAberto_thenOTabelaCaiNaTelaDeNaoExiste", async () => {
    renderAtRoute(<AppRoutes />, "/categorias/inventado/demais");

    expect(
      await screen.findByRole("heading", { name: "Esta tela não existe" }),
    ).toBeInTheDocument();
  });

  it("givenUmEnderecoQueExiste_whenAberto_thenNaoCaiNaTelaDeNaoExiste", () => {
    // O endereço de uma tela de verdade: ela entra por `lazy`, então o que aparece primeiro é o
    // indicador de carregamento — e o que importa é que **não** é a tela de "não existe". Sem esta
    // metade, uma rota `*` posta no lugar errado engoliria o catálogo inteiro sem ninguém notar.
    //
    // `/categorias` e não `/videos/novo`: apagar a rota de `videos/novo` deixa `videos/:id` casando
    // com ela (com id "novo"), e aí o teste não acusaria a falta. Nenhuma outra rota cobre
    // `/categorias`.
    renderAtRoute(<AppRoutes />, "/categorias");

    expect(screen.getByLabelText("carregando a tela")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Esta tela não existe" })).not.toBeInTheDocument();
  });
});
