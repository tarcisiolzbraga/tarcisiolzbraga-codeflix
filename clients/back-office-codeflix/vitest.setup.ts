import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach, vi } from "vitest";

// As URLs da admin-api e do Keycloak não precisam de nada aqui: o Vitest usa o próprio vite.config,
// e o `define` dele já substitui `import.meta.env.VITE_*` nos testes também — pelos mesmos valores
// que o desenvolvimento usa, derivados do `.env` da raiz.

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(() => {
  cleanup();

  // O `localStorage` do jsdom é um só para todos os testes de um arquivo. Sem limpar, a escolha de
  // tema que um teste faz vira o estado inicial do próximo — foi o que aconteceu com o `ThemeToggle`,
  // e o sintoma aparece no teste seguinte, não no que mexeu.
  localStorage.clear();
});
