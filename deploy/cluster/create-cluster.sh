#!/usr/bin/env bash
#
# O cluster local, em k3d (k3s dentro do Docker).
#
# Script, e não uma lista de comandos no README: o cluster é recriado com frequência enquanto os
# manifestos são escritos, e um passo esquecido entre uma recriação e outra é defeito que aparece
# meia hora depois, do outro lado.
#
# Por que k3d: o Docker já está aqui, e o k3s sobe em segundos. `minikube` traria uma VM, e `kind`
# não tem o balanceador que mapeia porta do host para o ingress sem passo extra.
set -euo pipefail

CLUSTER="${CLUSTER:-codeflix}"
# A porta do host que chega no gateway do Istio. **Não** 80: o compose do projeto ocupa 3000, 3001,
# 8080-8084 e 8025, e uma colisão aqui falharia na criação do cluster com uma mensagem do Docker que
# não diz de quem é a porta.
PORTA_HTTP="${PORTA_HTTP:-8090}"
# As 3000 e 3001 vão direto aos NodePorts dos dois fronts, por fora do gateway: o login precisa de
# contexto seguro, e `localhost` é o único endereço que o navegador trata assim sem TLS. O porquê
# está em deploy/apps/fronts-nodeport.yaml.

if k3d cluster list "$CLUSTER" >/dev/null 2>&1; then
  echo "o cluster '$CLUSTER' já existe; nada a fazer (use 'k3d cluster delete $CLUSTER' para refazer)"
  exit 0
fi

k3d cluster create "$CLUSTER" \
  --servers 1 \
  --agents 2 \
  --port "${PORTA_HTTP}:80@loadbalancer" \
  --port "80:80@loadbalancer" \
  --port "3000:30000@loadbalancer" \
  --port "3001:30001@loadbalancer" \
  --k3s-arg "--disable=traefik@server:*" \
  --wait

# O Traefik sai porque o ingress é do Istio. Deixar os dois faria duas coisas disputarem a porta 80
# do balanceador, e qual ganha depende da ordem de subida — defeito intermitente por configuração.

kubectl config use-context "k3d-${CLUSTER}"
kubectl wait --for=condition=Ready nodes --all --timeout=120s

echo
echo "cluster '$CLUSTER' pronto. O gateway do Istio responderá em http://localhost:${PORTA_HTTP}"
kubectl get nodes -o wide
