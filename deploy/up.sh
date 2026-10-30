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
#   3. as imagens vão para o registro — antes do Istio, porque nesta rede os nós não baixam nada,
#                                       e as de plataforma também vêm de lá
#   4. a malha instala                — e só então existe o Service do gateway
#   5. o DNS do cluster aponta para o gateway  — precisa do IP dele, que acabou de nascer
#   6. o segredo do .env e as identidades  — tudo depois disto os consome; pod que referencia
#                                       service account inexistente não é criado
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

passo "3/10 imagens no registro"
# As imagens têm de existir no host, e **construídas para o cluster**: as URLs das APIs são embutidas
# no build dos fronts. `deploy/build-images.sh` é quem passa os nomes do gateway; um
# `docker compose build` comum produz fronts apontando para localhost.
#
# Push, e não mais carga por tarball: o registro nasceu junto do cluster no passo 1. O script
# explica por que a troca importa — imagem com origem sobrevive à coleta do kubelet.
bash deploy/cluster/push-images.sh

passo "4/10 malha e painéis"
bash deploy/mesh/install-istio.sh

passo "5/10 DNS do cluster apontando para o gateway"
bash deploy/cluster/dns-localtest.sh

passo "6/10 segredo do .env e as identidades"
bash deploy/cluster/create-secret.sh
# **Antes de qualquer Deployment**, e não junto deles: todo pod daqui para baixo declara
# `serviceAccountName`, e pod que referencia service account inexistente não é criado — o kubelet
# responde "error looking up service account" e o rollout fica pendurado sem dizer o porquê.
kubectl apply -f deploy/mesh/service-accounts.yaml

passo "7/10 Keycloak"
bash deploy/infra/apply-keycloak.sh

passo "8/10 infra, aplicativos e rotas"
bash deploy/infra/apply-infra.sh
# **`envsubst`, porque os manifestos trazem `${TAG}`.** A tag é a do commit, e é ela que torna o
# `rollout undo` real: sem isso a revisão anterior apontaria para a mesma `latest`, e desfazer não
# desfaria nada.
#
# O preço é que um `kubectl apply -f` à mão precisa do `TAG` no ambiente. Está dito no README.
TAG="$(bash deploy/tag.sh)" envsubst '${TAG}' < deploy/apps/java.yaml | kubectl apply -f -
TAG="$(bash deploy/tag.sh)" envsubst '${TAG}' < deploy/apps/go-and-fronts.yaml | kubectl apply -f -
kubectl apply -f deploy/apps/fronts-nodeport.yaml
kubectl apply -f deploy/mesh/routes.yaml -f deploy/mesh/dashboards.yaml \
  -f deploy/mesh/peer-authentication.yaml -f deploy/mesh/authorization-policies.yaml
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
