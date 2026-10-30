#!/usr/bin/env bash
#
# Empurra as imagens para o registro do cluster.
#
# **Isto substituiu o carregamento por tarball**, e a troca vale ser contada porque o caminho antigo
# era o ponto mais frágil do deploy. Ele fazia `docker save` de tudo num arquivo de centenas de MB,
# copiava uma vez por nó e rodava `ctr images import` dentro de cada um. Três cópias por recriação.
#
# Pior que lento: imagem carregada assim **não tem origem**. Quando o disco passou de 85%, o kubelet
# coletou as imagens "não usadas" e elas não tinham de onde voltar — 46 pods despejados, e os pods
# seguintes em ImagePullBackOff buscando no Docker Hub uma imagem que só existia naquela máquina. O
# rótulo `io.cri-containerd.pinned=true` foi tentado e **não resistiu** à pressão.
#
# Com registro, a coleta do kubelet é inofensiva: ele remove e busca de novo.
#
# **As de plataforma vêm junto, e é isso que permite apagar o tarball por completo.** Os nós não têm
# saída para a internet nesta rede, então Istio, Kiali, Prometheus, Grafana, Jaeger e o
# metrics-server também precisam vir daqui. O `registries.yaml` espelha `docker.io`, `quay.io` e
# `ghcr.io` para este registro, então basta empurrá-las com o caminho de origem — o nó pede pelo
# nome de sempre e o espelho o traz para cá.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

REGISTRO="${REGISTRO:-localhost:5000}"
# A mesma fonte que o build usou: se as duas divergirem, o cluster pede uma imagem que ninguém
# empurrou, e o sintoma aparece como ImagePullBackOff longe daqui.
TAG="${TAG:-$(bash deploy/tag.sh)}"

# As nossas, construídas pelo compose. São as que mudam a cada commit.
NOSSAS=(
  admin-codeflix-admin-api
  admin-codeflix-videos-api
  admin-codeflix-subscription-api
  admin-codeflix-encoder-api
  admin-codeflix-mail-api
  admin-codeflix-portal-codeflix
  admin-codeflix-back-office-codeflix
)

# As de fora, com o caminho **de origem** preservado: o espelho casa pelo caminho, então
# `quay.io/kiali/kiali:v2.31` tem de virar `localhost:5000/kiali/kiali:v2.31`, e não outra coisa.
# A de sandbox entra porque sem ela **nenhum** pod sai de Init.
DE_FORA=(
  rancher/mirrored-pause:3.6
  istio/pilot:1.31.3
  istio/proxyv2:1.31.3
  quay.io/kiali/kiali:v2.31
  prom/prometheus:v3.10.0
  grafana/grafana:12.0.1
  jaegertracing/jaeger:2.17.0
  ghcr.io/prometheus-operator/prometheus-config-reloader:v0.89.0
  rancher/mirrored-metrics-server:v0.8.1
)

# O caminho que o espelho espera: sem o host de origem, porque o nó já pede ao espelho.
caminho_no_registro() {
  local imagem="$1"
  case "$imagem" in
    quay.io/*|ghcr.io/*|docker.io/*) echo "${REGISTRO}/${imagem#*/}" ;;
    *)                               echo "${REGISTRO}/${imagem}" ;;
  esac
}

# Imagem que não está no host falha aqui, com o nome dela — e não lá na frente, como
# ImagePullBackOff num pod qualquer.
faltando=0
for imagem in "${NOSSAS[@]/%/:$TAG}" "${DE_FORA[@]}"; do
  docker image inspect "$imagem" >/dev/null 2>&1 || { echo "   FALTA no host: $imagem" >&2; faltando=1; }
done
if [ "$faltando" != 0 ]; then
  echo >&2
  echo "construa as nossas com 'bash deploy/build-images.sh' e traga as de fora com" >&2
  echo "'bash deploy/cluster/export-platform-images.sh'." >&2
  exit 1
fi

echo "==> empurrando para ${REGISTRO} (tag ${TAG})"
# As nossas vão com **as duas** tags: a do commit, que é a que o manifesto referencia, e a `latest`,
# que mantém o `docker compose up` local funcionando sem precisar saber de tag nenhuma.
for nome in "${NOSSAS[@]}"; do
  for t in "$TAG" latest; do
    docker tag "${nome}:${TAG}" "${REGISTRO}/${nome}:${t}"
    docker push -q "${REGISTRO}/${nome}:${t}" >/dev/null
  done
  echo "   ${REGISTRO}/${nome}:${TAG} (+ latest)"
done
for imagem in "${DE_FORA[@]}"; do
  destino="$(caminho_no_registro "$imagem")"
  docker tag "$imagem" "$destino"
  docker push -q "$destino" >/dev/null
  echo "   $destino"
done

# **Confere no registro, e não na saída do push.** O `docker push` já respondeu sucesso com o
# registro recusando por trás, e o efeito aparece bem depois: um pod em ImagePullBackOff.
echo "==> conferindo o catálogo do registro"
catalogo="$(curl -sf "http://${REGISTRO}/v2/_catalog" || true)"
falta=0
for imagem in "${NOSSAS[@]/%/:$TAG}" "${DE_FORA[@]}"; do
  destino="$(caminho_no_registro "$imagem")"
  repo="${destino#"${REGISTRO}/"}"; repo="${repo%:*}"
  printf '%s' "$catalogo" | grep -q "\"$repo\"" || { echo "   NAO CHEGOU: $repo" >&2; falta=1; }
done
if [ "$falta" != 0 ]; then
  echo "alguma imagem não entrou no registro. Rode de novo; se insistir, confira se o registro" >&2
  echo "'k3d-codeflix-registry' está de pé com 'k3d registry list'." >&2
  exit 1
fi
echo "   todas as $(( ${#NOSSAS[@]} + ${#DE_FORA[@]} )) presentes"
