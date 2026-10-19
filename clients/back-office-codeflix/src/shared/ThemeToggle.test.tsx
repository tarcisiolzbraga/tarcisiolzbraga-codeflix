import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ThemeProvider } from "@mui/material/styles";
import { describe, expect, it } from "vitest";
import { theme } from "@/shared/theme";
import { ThemeToggle } from "@/shared/ThemeToggle";

function renderiza() {
  return render(
    <ThemeProvider theme={theme} defaultMode="dark">
      <ThemeToggle />
    </ThemeProvider>,
  );
}

// **Três estados, e não dois.** O padrão é escuro, porque é um catálogo de vídeo usado por horas; o
// terceiro existe para quem quer a preferência do sistema, e para haver como voltar atrás.
describe("ThemeToggle", () => {
  it("givenNinguemMexeu_whenRenderizado_thenOPainelAbreNoEscuro", () => {
    renderiza();

    expect(screen.getByRole("button", { name: "Tema: escuro" })).toBeInTheDocument();
  });

  it("givenOEscuro_whenClicado_thenVaiParaOClaro", async () => {
    renderiza();

    await userEvent.click(screen.getByRole("button", { name: "Tema: escuro" }));

    expect(await screen.findByRole("button", { name: "Tema: claro" })).toBeInTheDocument();
  });

  // A volta completa: escuro → claro → sistema → escuro. O terceiro estado é o que importa aqui —
  // **existe um jeito de voltar a seguir o sistema** depois de ter escolhido claro ou escuro. Sem
  // ele, a escolha seria irreversível.
  it("givenTresCliques_whenPercorridos_thenVoltaAoEscuro", async () => {
    renderiza();

    await userEvent.click(screen.getByRole("button", { name: "Tema: escuro" }));
    await userEvent.click(await screen.findByRole("button", { name: "Tema: claro" }));
    await userEvent.click(await screen.findByRole("button", { name: "Tema: o do sistema" }));

    expect(await screen.findByRole("button", { name: "Tema: escuro" })).toBeInTheDocument();
  });
});
