import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach, vi } from "vitest";

// A URL da API vem do next.config em tempo de build, e o Vitest não passa por ele: sem isto o
// módulo de configuração estouraria ao ser importado, dizendo que a variável não foi definida — que
// é exatamente o que ele promete fazer.
process.env.NEXT_PUBLIC_SUBSCRIPTION_API_URL ??= "http://localhost:8084";
process.env.NEXT_PUBLIC_CATALOG_API_URL ??= "http://localhost:8082";
process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER ??= "http://localhost:8081/realms/codeflix";
process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ??= "front-codeflix";

// Desmonta a árvore e limpa dublê entre casos: sem isso um teste vê a tela do anterior, e um fetch
// dublado sobrevive para o seguinte.
beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(() => {
  cleanup();
});
