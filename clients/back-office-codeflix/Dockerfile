# A imagem do back-office. Dois estágios: o build, e um nginx com o resultado.
#
# **As URLs entram como `build-arg`, e não como variável de ambiente do container.** O Vite as
# substitui no código em tempo de build, pelo `define` do `vite.config.ts` — passar a URL no `run`
# não mudaria nada, porque o valor já está no JavaScript que o browser baixa. Uma imagem construída
# para homologação não serve para produção.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# npm ci, e não npm install: respeita o lock e falha se ele estiver fora de sincronia.
RUN npm ci
COPY . .

# Sem valor: cada um cai para o padrão de desenvolvimento do `vite.config.ts`, que é
# `http://localhost:<porta>`. É o que a imagem local quer, porque a porta da admin-api é publicada no
# host e é o **browser** que a alcança.
ARG PUBLIC_ADMIN_API_URL
ARG PUBLIC_KEYCLOAK_ISSUER
ARG APP_PORT
ARG KEYCLOAK_HOST
ARG KEYCLOAK_PORT
ARG KEYCLOAK_REALM
ARG FRONT_KEYCLOAK_CLIENT_ID
ARG MEDIA_MAX_FILE_SIZE

RUN npm run build

FROM nginx:1.29-alpine AS runner

# Pega as correções de segurança publicadas depois de a base ser construída. Aqui isso não é
# detalhe: o Trivy achou 46 CVEs HIGH/CRITICAL nos pacotes de sistema desta base — curl, pcre2,
# brotli e companhia —, todos com correção já publicada no repositório do Alpine.
RUN apk -U upgrade --no-cache

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 3001
