import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PlanCard } from "@/plans/PlanCard";
import type { PlanOnSale } from "@/plans/plans";

const basic: PlanOnSale = {
  id: "11111111-1111-1111-1111-111111111111",
  name: "Básico",
  description: "Um aparelho, resolução padrão.",
  currency: "BRL",
  price: 29.9,
};

describe("PlanCard", () => {
  it("givenAPlanOnSale_whenRenders_thenShowsNameDescriptionAndPrice", () => {
    // Given um plano à venda
    // When o cartão é renderizado
    render(<PlanCard plan={basic} />);

    // Then quem escolhe vê os três dados de que precisa
    expect(screen.getByRole("heading", { name: "Básico" })).toBeInTheDocument();
    expect(screen.getByText("Um aparelho, resolução padrão.")).toBeInTheDocument();
    expect(screen.getByText(/29,90/)).toBeInTheDocument();
  });

  it("givenAPlanOnSale_whenRenders_thenSaysThePriceIsMonthly", () => {
    // Given um plano à venda
    // When o cartão é renderizado
    render(<PlanCard plan={basic} />);

    // Then o período está na tela: preço sem período não diz nada a quem vai assinar
    expect(screen.getByText("/mês")).toBeInTheDocument();
  });
});
