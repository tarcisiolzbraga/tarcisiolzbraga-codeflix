# A imagem do portal. Três estágios, e o motivo de cada um:
#
#   deps   instala com o package-lock, e fica em cache enquanto ele não muda
#   build  roda o `next build`, que é onde as URLs públicas são **embutidas**
#   runner carrega só o servidor e o que ele usa
#
# **As URLs entram como `build-arg`, e não como variável de ambiente do container**, e isso não é
# escolha de estilo: `NEXT_PUBLIC_*` é embutido no pacote em tempo de build. Uma imagem construída
# para homologação não serve para produção, e passar a URL no `run` não mudaria nada — o valor já
# está no JavaScript que o browser baixa.
FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
# npm ci, e não npm install: respeita o lock e falha se ele estiver fora de sincronia.
RUN npm ci

FROM node:24-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Sem valor: cada um cai para o padrão de desenvolvimento do `buildEnv.ts`, que é
# `http://localhost:<porta>`. É o que a imagem local quer, porque as portas das APIs são publicadas
# no host e é o **browser** que as alcança.
ARG PUBLIC_SUBSCRIPTION_API_URL
ARG PUBLIC_CATALOG_API_URL
ARG PUBLIC_ADMIN_API_URL
ARG PUBLIC_KEYCLOAK_ISSUER
ARG SUBSCRIPTION_APP_PORT
ARG CATALOG_APP_PORT
ARG APP_PORT
ARG KEYCLOAK_HOST
ARG KEYCLOAK_PORT
ARG KEYCLOAK_REALM
ARG FRONT_KEYCLOAK_CLIENT_ID

# A telemetria do Next é ligada por padrão e manda dado de build para fora. Aqui ela fica desligada:
# o que sai do build é escolha, não padrão herdado.
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:24-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Pega as correções de segurança publicadas depois de a base ser construída.
RUN apk -U upgrade --no-cache

# **O npm sai da imagem de execução.** O CMD abaixo é `node server.js`: npm não roda aqui, e o que
# ele deixava para trás eram cinco CVEs HIGH medidos pelo Trivy — undici, brace-expansion e
# http-cache-semantics, todos em `node_modules/npm/node_modules/`, isto é, dependências **do npm**,
# não deste projeto.
#
# É por isso que `npm audit` dizia zero enquanto a imagem tinha cinco: as duas ferramentas respondem
# perguntas diferentes — uma olha o package-lock, a outra olha o que está dentro da imagem.
RUN rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx

# Usuário sem privilégio: o container não precisa de root para servir arquivo.
RUN addgroup -S portal && adduser -S portal -G portal
USER portal

# O `output: "standalone"` do next.config monta esta pasta com o servidor e **só** as dependências
# que ele de fato usa — em vez do `node_modules` inteiro.
COPY --from=build --chown=portal:portal /app/.next/standalone ./
COPY --from=build --chown=portal:portal /app/.next/static ./.next/static
COPY --from=build --chown=portal:portal /app/public ./public

EXPOSE 3000
CMD ["node", "server.js"]
