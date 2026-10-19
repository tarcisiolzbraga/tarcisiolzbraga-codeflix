import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/shared/testing";

vi.mock("react-oidc-context", () => ({ useAuth: () => ({ user: { access_token: "abc.123" } }) }));

const { NotFound } = await import("@/shared/NotFound");

// Sem esta tela, uma URL errada caía numa tela **vazia** — e tela vazia num painel de administração
// parece defeito do sistema, não endereço errado.
describe("NotFound", () => {
  it("givenUmEnderecoQueNaoExiste_whenAbre_thenDizQueNaoExisteEQualEra", () => {
    renderWithProviders(<NotFound />, { path: "*", route: "/categorias/inventado/demais" });

    expect(screen.getByRole("heading", { name: "Esta tela não existe" })).toBeInTheDocument();
    // O endereço aparece porque link quebrado em e-mail ou anotação é a causa comum. O seletor é o
    // `<code>` porque o helper de teste também desenha o caminho atual, num `span` próprio.
    expect(document.querySelector("code")).toHaveTextContent("/categorias/inventado/demais");
  });

  it("givenATela_whenAbre_thenOfereceOCaminhoDeVolta", () => {
    renderWithProviders(<NotFound />, { path: "*", route: "/nao-existe" });

    expect(screen.getByRole("link", { name: /Ir para as categorias/ })).toHaveAttribute(
      "href",
      "/categorias",
    );
  });
});
