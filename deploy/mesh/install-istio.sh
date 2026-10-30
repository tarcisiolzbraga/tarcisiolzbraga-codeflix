#!/usr/bin/env bash
#
# A malha: Istio, e os painéis que leem o que ela mede.
#
# `istioctl install` com o perfil `demo`, e não `default`: é o perfil que liga a telemetria que o
# Kiali precisa para desenhar o grafo, e que o `default` deixa de fora. Para um cluster de
# desenvolvimento é o certo; num cluster de verdade seria `default` mais o que se quisesse medir.
set -euo pipefail

VERSAO="${VERSAO:-1.31.3}"

# **O pacote mora em cache durável, e é baixado se faltar.**
#
# Ele apontava para um diretório sob `/tmp`, e isso quebrou: o caminho era o temporário de uma
# sessão, que some na limpeza. O `up.sh` ia até o passo 4, instalava a malha e morria no primeiro
# addon com "does not exist" — num cluster já criado, no meio do caminho.
#
# `~/.cache` porque é o lugar certo para arquivo grande que se rebaixa sozinho: não polui o
# repositório, sobrevive a reinício, e apagar não perde nada além de um download.
#
# **Só os addons precisam do pacote.** O `istioctl install` não o usa — o binário basta. Mas os
# painéis são YAML de exemplo que só vem no tarball, e tem de ser o da **mesma versão** da malha:
# misturar versão de addon com versão de Istio é a origem de "o Kiali não vê nada" com tudo
# aparentemente certo.
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/codeflix"
PACOTE="${PACOTE:-${CACHE}/istio-${VERSAO}}"

if [ ! -d "${PACOTE}/samples/addons" ]; then
  echo "==> baixando o pacote do Istio ${VERSAO} (só os painéis precisam dele)"
  mkdir -p "$CACHE"
  TAR="${CACHE}/istio-${VERSAO}.tar.gz"
  curl -fsSL --retry 3 -o "$TAR" \
    "https://github.com/istio/istio/releases/download/${VERSAO}/istio-${VERSAO}-linux-amd64.tar.gz"
  tar -xzf "$TAR" -C "$CACHE"
  rm -f "$TAR"
  # Confere o que vai ser usado, e não que o tar terminou: tarball truncado extrai sem erro.
  [ -d "${PACOTE}/samples/addons" ] || {
    echo "o pacote baixou mas não tem samples/addons em ${PACOTE}" >&2
    exit 1
  }
fi

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

# O recurso que faz os sidecars **mandarem** span. Depois dos addons de propósito: ele aponta para o
# `jaeger-collector`, e a CRD `Telemetry` vem com a malha. Sem ele o Jaeger sobe vazio e fica vazio.
kubectl apply -f "$(dirname "${BASH_SOURCE[0]}")/telemetry.yaml"

kubectl -n istio-system wait --for=condition=Available deployment --all --timeout=300s
kubectl -n istio-system get deploy
