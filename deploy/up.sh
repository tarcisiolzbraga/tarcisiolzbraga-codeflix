#!/usr/bin/env bash
#
# Sobe o Codeflix inteiro num cluster novo, do zero, com um comando.
#
#   bash deploy/up.sh
#
# A ordem **não** é arbitrária, e cada passo depende do anterior de um jeito que não é óbvio:
#
#   1. o cluster existe
#   2. os nós resolvem DNS            — senão nem a imagem de sandbox chega, e todo pod fica em Init
#   3. as imagens entram nos nós      — antes do Istio, porque nesta rede os nós não baixam nada
#   4. a malha instala                — e só então existe o Service do gateway
#   5. o DNS do cluster aponta para o gateway  — precisa do IP dele, que acabou de nascer
#   6. o segredo do .env              — tudo depois disto o consome
#   7. Keycloak primeiro              — o `iss` dos tokens depende do nome dele estar de pé
#   8. a infra, os aplicativos, as rotas
#   9. o conector de CDC              — o Kafka nasce vazio; sem isto o catálogo fica vazio para sempre
#  10. a semeadura                    — cluster recriado nasce sem dado nenhum
#
# Idempotente onde dá: rodar de novo num cluster existente não quebra, só reaplica.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
export PATH="$HOME/.local/bin:$PATH"

passo() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }

passo "1/10 cluster"
bash deploy/cluster/create-cluster.sh

passo "2/10 resolvedor dos nós"
bash deploy/cluster/dns-nodes.sh

passo "3/10 imagens nos nós"
# As imagens têm de existir no host, e **construídas para o cluster**: as URLs das APIs são embutidas
# no build dos fronts. `deploy/build-images.sh` é quem passa os nomes do gateway; um
# `docker compose build` comum produz fronts apontando para localhost.
bash deploy/cluster/import-images.sh

passo "4/10 malha e painéis"
bash deploy/mesh/install-istio.sh

passo "5/10 DNS do cluster apontando para o gateway"
bash deploy/cluster/dns-localtest.sh

passo "6/10 segredo do .env"
bash deploy/cluster/create-secret.sh

passo "7/10 Keycloak"
bash deploy/infra/apply-keycloak.sh

passo "8/10 infra, aplicativos e rotas"
bash deploy/infra/apply-infra.sh
kubectl apply -f deploy/apps/java.yaml -f deploy/apps/go-and-fronts.yaml -f deploy/apps/fronts-nodeport.yaml
kubectl apply -f deploy/mesh/routes.yaml -f deploy/mesh/dashboards.yaml
echo "esperando os pods..."
kubectl -n codeflix wait --for=condition=Available deployment --all --timeout=600s

passo "9/10 conector de CDC"
bash deploy/infra/register-cdc.sh

passo "10/10 semeadura"
ADMIN_URL=http://admin-api.localtest.me \
ASSINATURA_URL=http://assinatura.localtest.me \
KEYCLOAK_URL=http://keycloak.localtest.me \
MAILPIT_URL=http://mailpit.localtest.me \
python3 deploy/seed/seed.py

cat <<'FIM'

pronto.

  portal          http://localhost:3000     (NodePort: o login precisa de contexto seguro)
  back-office     http://localhost:3001
  admin-api       http://admin-api.localtest.me
  catálogo        http://catalogo.localtest.me/api/graphql
  assinatura      http://assinatura.localtest.me
  Keycloak        http://keycloak.localtest.me
  Mailpit         http://mailpit.localtest.me
  RabbitMQ        http://rabbitmq.localtest.me
  Kiali           http://kiali.localtest.me/kiali/
  Grafana         http://grafana.localtest.me
  Jaeger          http://jaeger.localtest.me/jaeger/search

  back-office:  operador-de-teste / operador-de-teste
  portal:       ana.ribeiro@codeflix.com.br / uma-senha-boa
FIM
