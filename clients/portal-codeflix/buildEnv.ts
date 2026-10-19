import { existsSync } from "node:fs";
import { resolve } from "node:path";

// De onde as URLs que o pacote embute vêm, em **um** lugar — o `next.config.ts` as usa, e um teste
// afirma as regras.
//
// O monorepo tem um `.env` só, na raiz, e o portal o lê de lá como os serviços Java fazem pelo
// `spring.config.import`. Opcional, como o `optional:file:` deles: num build em container o arquivo
// não está no contexto, e os valores chegam como variável de ambiente de verdade.
export function loadRootEnv(from: string = process.cwd()): void {
  const rootEnv = resolve(from, "..", "..", ".env");
  if (existsSync(rootEnv)) {
    process.loadEnvFile(rootEnv);
  }
}

// Uma URL pública, com duas fontes e nesta ordem:
//
// 1. a **URL inteira**, quando ela é dada — é assim que se constrói a imagem de homologação ou de
//    produção, onde as APIs não estão em `localhost`;
// 2. `http://localhost:<porta>`, derivada da mesma porta que o compose usa — o padrão de
//    desenvolvimento, e o que vale na máquina de quem está programando.
//
// A ordem importa: sem a primeira fonte, **toda** imagem aponta para `localhost`, e uma construída
// para outro ambiente é impossível. Era o caso até aqui.
export function publicUrl(url: string | undefined, port: string | undefined, fallbackPort: string): string {
  const explicit = url?.trim();
  return explicit !== undefined && explicit !== ""
    ? explicit.replace(/\/+$/, "")
    : `http://localhost:${port ?? fallbackPort}`;
}

// O emissor do Keycloak, montado das mesmas variáveis que o `keycloak.issuer-uri` dos serviços usa:
// divergir aqui faria o token ser recusado pelo `iss`, que é o defeito que o `.env` único existe
// para evitar. Também aceita a URL inteira, pelo mesmo motivo de cima.
export function keycloakIssuer(url: string | undefined = process.env.PUBLIC_KEYCLOAK_ISSUER): string {
  const explicit = url?.trim();
  if (explicit !== undefined && explicit !== "") {
    return explicit.replace(/\/+$/, "");
  }
  const host = process.env.KEYCLOAK_HOST ?? "localhost";
  const port = process.env.KEYCLOAK_PORT ?? "8081";
  const realm = process.env.KEYCLOAK_REALM ?? "codeflix";
  return `http://${host}:${port}/realms/${realm}`;
}
