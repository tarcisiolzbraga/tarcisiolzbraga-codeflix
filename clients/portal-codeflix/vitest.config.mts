import react from "@vitejs/plugin-react";
import { resolve } from "node:path";
import { defineConfig } from "vitest/config";

// Vitest, e não o Jest da referência do curso: com o Next 16 e o TypeScript atual, o Jest pede
// transformador e configuração à parte, e o Vitest lê o mesmo tsconfig e o mesmo alias "@/".
//
// Componente de servidor assíncrono (como a página de planos) **não** é testável aqui — nem no Jest:
// o próprio Next recomenda teste de ponta a ponta para isso. Então o que este arquivo cobre é o que
// dá para cobrir de verdade: a conversa com a API, a formatação e a apresentação.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
  resolve: {
    alias: { "@": resolve(import.meta.dirname, "src") },
  },
});
