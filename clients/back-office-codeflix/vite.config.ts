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

// Uma URL pública, com duas fontes e nesta ordem:
//
// 1. a **URL inteira**, quando ela é dada — é assim que se constrói a imagem de homologação ou de
//    produção, onde a API não está em `localhost`;
// 2. `http://localhost:<porta>`, derivada da mesma porta que o compose usa — o padrão de
//    desenvolvimento.
//
// A ordem importa: sem a primeira fonte, **toda** imagem aponta para `localhost`, e uma construída
// para outro ambiente é impossível. Era o caso até aqui.
const publicUrl = (url: string | undefined, port: string | undefined, fallbackPort: string) => {
  const explicit = url?.trim();
  return explicit !== undefined && explicit !== ""
    ? explicit.replace(/\/+$/, "")
    : `http://localhost:${port ?? fallbackPort}`;
};

// O emissor, montado das mesmas variáveis que o `keycloak.issuer-uri` dos serviços usa: divergir aqui
// faria o token ser recusado pelo `iss`, que é o defeito que o .env único existe para evitar. Aceita
// a URL inteira pelo mesmo motivo de cima.
const keycloakIssuer = () => {
  const explicit = process.env.PUBLIC_KEYCLOAK_ISSUER?.trim();
  if (explicit !== undefined && explicit !== "") {
    return explicit.replace(/\/+$/, "");
  }
  const host = process.env.KEYCLOAK_HOST ?? "localhost";
  const port = process.env.KEYCLOAK_PORT ?? "8081";
  const realm = process.env.KEYCLOAK_REALM ?? "codeflix";
  return `http://${host}:${port}/realms/${realm}`;
};

// O teto de upload vem do `.env` no formato do Spring (`100MB`), e o browser precisa de bytes. A
// conversão fica aqui, do lado que lê o arquivo, porque é aqui que o formato é conhecido.
const maxFileSize = () => {
  const bruto = (process.env.MEDIA_MAX_FILE_SIZE ?? "100MB").trim();
  const partes = /^(\d+)\s*(B|KB|MB|GB)?$/i.exec(bruto);
  if (partes === null) {
    throw new Error(`MEDIA_MAX_FILE_SIZE precisa ser algo como '100MB'; veio '${bruto}'`);
  }
  const fatores: Record<string, number> = { B: 1, KB: 1024, MB: 1024 ** 2, GB: 1024 ** 3 };
  return Number(partes[1]) * fatores[(partes[2] ?? "B").toUpperCase()]!;
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
      publicUrl(process.env.PUBLIC_ADMIN_API_URL, process.env.APP_PORT, "8080"),
    ),
    "import.meta.env.VITE_KEYCLOAK_ISSUER": JSON.stringify(keycloakIssuer()),
    "import.meta.env.VITE_KEYCLOAK_CLIENT_ID": JSON.stringify(
      process.env.FRONT_KEYCLOAK_CLIENT_ID ?? "front-codeflix",
    ),
    "import.meta.env.VITE_MEDIA_MAX_FILE_SIZE": JSON.stringify(String(maxFileSize())),
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
