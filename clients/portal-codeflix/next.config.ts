import type { NextConfig } from "next";
import { keycloakIssuer, loadRootEnv, publicUrl } from "./buildEnv";

// As regras de onde cada URL vem estão no `buildEnv.ts`, com teste. Aqui ficam só as chaves que o
// Next embute.
loadRootEnv();

const nextConfig: NextConfig = {
  // `standalone` para a imagem de produção carregar só o que o servidor precisa, em vez de
  // `node_modules` inteiro: o `next build` monta a pasta `.next/standalone` com o servidor e as
  // dependências que ele de fato usa.
  output: "standalone",
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
  // imagem construída para homologação não serve para produção. Por isso cada URL aceita o valor
  // inteiro — é com ele que o build de cada ambiente é feito —, e cai para `localhost:<porta>` do
  // `.env` quando ele não vem, que é o padrão de desenvolvimento.
  env: {
    NEXT_PUBLIC_SUBSCRIPTION_API_URL: publicUrl(
      process.env.PUBLIC_SUBSCRIPTION_API_URL,
      process.env.SUBSCRIPTION_APP_PORT,
      "8084",
    ),
    NEXT_PUBLIC_CATALOG_API_URL: publicUrl(
      process.env.PUBLIC_CATALOG_API_URL,
      process.env.CATALOG_APP_PORT,
      "8082",
    ),
    NEXT_PUBLIC_ADMIN_API_URL: publicUrl(
      process.env.PUBLIC_ADMIN_API_URL,
      process.env.APP_PORT,
      "8080",
    ),
    NEXT_PUBLIC_KEYCLOAK_ISSUER: keycloakIssuer(),
    // O client público do realm, com authorization code + PKCE S256. Não tem segredo: é o que
    // permite ele viver no browser.
    NEXT_PUBLIC_KEYCLOAK_CLIENT_ID: process.env.FRONT_KEYCLOAK_CLIENT_ID ?? "front-codeflix",
  },
};

export default nextConfig;
