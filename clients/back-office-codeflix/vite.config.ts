import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { existsSync } from "node:fs";
import { resolve } from "node:path";
// `defineConfig` do vitest/config, e não do vite: é ele que conhece a chave `test`.
import { defineConfig } from "vitest/config";

// O monorepo tem **um `.env` só**, na raiz, e este cliente o lê de lá como o portal faz pelo
// next.config e como os serviços Java fazem pelo spring.config.import. Sem isto haveria uma segunda
// fonte de verdade para as portas, e o dia em que APP_PORT mudasse o back-office continuaria batendo
// na antiga.
//
// Opcional, como o `optional:file:` do Spring: em homologação e produção não há arquivo, e os valores
// chegam como variável de ambiente de verdade.
const rootEnv = resolve(import.meta.dirname, "..", "..", ".env");
if (existsSync(rootEnv)) {
  process.loadEnvFile(rootEnv);
}

// O emissor, montado das mesmas variáveis que o `keycloak.issuer-uri` dos serviços usa: divergir aqui
// faria o token ser recusado pelo `iss`, que é o defeito que o .env único existe para evitar.
const keycloakIssuer = () => {
  const host = process.env.KEYCLOAK_HOST ?? "localhost";
  const port = process.env.KEYCLOAK_PORT ?? "8081";
  const realm = process.env.KEYCLOAK_REALM ?? "codeflix";
  return `http://${host}:${port}/realms/${realm}`;
};

export default defineConfig({
  plugins: [react()],
  // O mesmo alias do portal, para o caminho de import dizer de onde a coisa vem.
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  // 3001 e não 3000: o portal do assinante ocupa a 3000, e os dois rodam juntos no
  // desenvolvimento. A porta está liberada no `redirectUris` do client do realm.
  server: { port: 3001, strictPort: true },
  // `define` em vez de `envDir` apontado para a raiz: o Vite só expõe variável com prefixo `VITE_`,
  // e o `.env` do monorepo não tem esse prefixo — ele descreve a infraestrutura, não este cliente.
  // Aqui os valores são derivados do que já existe lá.
  define: {
    "import.meta.env.VITE_ADMIN_API_URL": JSON.stringify(
      `http://localhost:${process.env.APP_PORT ?? "8080"}`,
    ),
    "import.meta.env.VITE_KEYCLOAK_ISSUER": JSON.stringify(keycloakIssuer()),
    "import.meta.env.VITE_KEYCLOAK_CLIENT_ID": JSON.stringify(
      process.env.FRONT_KEYCLOAK_CLIENT_ID ?? "front-codeflix",
    ),
  },
  // 500 kB é o padrão do Vite, pensado para pegar quem empacotou sem perceber. Aqui os dois pedaços
  // são Material e DataGrid, que custam isto mesmo — e o aviso disparando em todo build treinaria a
  // ignorar a saída. 700 kB ainda grita se uma dependência nova entrar.
  build: { chunkSizeWarningLimit: 700 },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
