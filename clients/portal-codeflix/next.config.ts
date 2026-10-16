import type { NextConfig } from "next";
import { existsSync } from "node:fs";
import { resolve } from "node:path";

// O monorepo tem **um `.env` só**, na raiz, e o portal o lê de lá como os serviços Java fazem pelo
// spring.config.import. Sem isto haveria uma segunda fonte de verdade para as portas, e o dia em que
// SUBSCRIPTION_APP_PORT mudasse o portal continuaria batendo na antiga.
//
// Opcional, como o `optional:file:` do Spring: em homologação e produção não há arquivo, e os valores
// chegam como variável de ambiente de verdade.
const rootEnv = resolve(process.cwd(), "..", "..", ".env");
if (existsSync(rootEnv)) {
  process.loadEnvFile(rootEnv);
}

// Com padrão de desenvolvimento, como o application-development.yml dos serviços: na máquina do
// desenvolvedor as três APIs sobem em localhost, e as portas vêm do mesmo .env do compose.
const apiUrl = (port: string | undefined, fallback: string) =>
  `http://localhost:${port ?? fallback}`;

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // NEXT_PUBLIC_* é embutido no pacote **em tempo de build**, não lido em tempo de execução: uma
  // imagem construída para homologação não serve para produção. Está certo para esta fatia, onde a
  // busca acontece no servidor; quando houver implantação em container, estas URLs entram no build
  // de cada ambiente.
  env: {
    NEXT_PUBLIC_SUBSCRIPTION_API_URL: apiUrl(process.env.SUBSCRIPTION_APP_PORT, "8084"),
    NEXT_PUBLIC_CATALOG_API_URL: apiUrl(process.env.CATALOG_APP_PORT, "8082"),
    NEXT_PUBLIC_ADMIN_API_URL: apiUrl(process.env.APP_PORT, "8080"),
  },
};

export default nextConfig;
