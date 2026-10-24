#!/usr/bin/env bash
#
# A malha: Istio, e os painéis que leem o que ela mede.
#
# `istioctl install` com o perfil `demo`, e não `default`: é o perfil que liga a telemetria que o
# Kiali precisa para desenhar o grafo, e que o `default` deixa de fora. Para um cluster de
# desenvolvimento é o certo; num cluster de verdade seria `default` mais o que se quisesse medir.
set -euo pipefail

VERSAO="${VERSAO:-1.31.3}"
PACOTE="${PACOTE:-/tmp/claude-1000/istio-${VERSAO}}"

istioctl install --set profile=demo --skip-confirmation

# A injeção do sidecar é **por namespace**, por rótulo. Sem o rótulo o pod sobe sem o Envoy ao lado e
# não aparece no grafo — e isso falha calado: o pod fica saudável, só invisível.
kubectl create namespace codeflix --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace codeflix istio-injection=enabled --overwrite

# Os painéis vêm do pacote do Istio, na versão do pacote: misturar versão de addon com versão de
# malha é a origem de "o Kiali não vê nada" com tudo aparentemente certo.
for addon in prometheus kiali jaeger grafana; do
  kubectl apply -f "${PACOTE}/samples/addons/${addon}.yaml"
done

kubectl -n istio-system wait --for=condition=Available deployment --all --timeout=300s
kubectl -n istio-system get deploy
