#!/usr/bin/env bash
#
# Põe as imagens locais dentro do containerd de **cada** nó.
#
# Por que não `k3d image import`: nesta máquina ele carregou as imagens só no nó servidor (e o modo
# `direct` falhou com "content digest ... not found"). Pod agendado num agente ficava em
# ImagePullBackOff tentando buscar `admin-codeflix-admin-api:latest` no Docker Hub, onde ela não
# existe — e os nós, além disso, não têm saída TCP para a internet nesta rede.
#
# O caminho longo é também o previsível: um `docker save` com todas as imagens, uma cópia por nó, e
# um `ctr images import` dentro de cada um. Verifica no fim, em vez de confiar na mensagem de sucesso.
#
# A alternativa melhor, para quando isto incomodar: um registro local do k3d
# (`k3d registry create`), com `registries.yaml` nos nós. Aí seria um `docker push` em vez de três
# cópias — mas exige recriar o cluster, e por isso não está aqui.
set -euo pipefail

CLUSTER="${CLUSTER:-codeflix}"
IMAGENS=(
  # as nossas, construídas pelo compose
  admin-codeflix-admin-api:latest
  admin-codeflix-videos-api:latest
  admin-codeflix-subscription-api:latest
  admin-codeflix-encoder-api:latest
  admin-codeflix-mail-api:latest
  admin-codeflix-portal-codeflix:latest
  admin-codeflix-back-office-codeflix:latest
  # a de sandbox: o k3d não a traz, e sem ela **nenhum** pod sai de Init
  rancher/mirrored-pause:3.6
  # as da plataforma, que os nós não conseguem baixar nesta rede. Vêm de
  # export-platform-images.sh; sem elas a malha não sobe num cluster novo.
  istio/pilot:1.31.3
  istio/proxyv2:1.31.3
  quay.io/kiali/kiali:v2.31
  prom/prometheus:v3.10.0
  grafana/grafana:12.0.1
  jaegertracing/jaeger:2.17.0
  ghcr.io/prometheus-operator/prometheus-config-reloader:v0.89.0
  rancher/mirrored-metrics-server:v0.8.1
)

# Uma imagem que não esteja no host derruba o `docker save` inteiro, e com mensagem que não diz qual.
for imagem in "${IMAGENS[@]}"; do
  docker image inspect "$imagem" >/dev/null 2>&1 || {
    echo "a imagem '$imagem' não está no Docker do host." >&2
    case "$imagem" in
      admin-codeflix-*) echo "  construa com: docker compose build" >&2 ;;
      *) echo "  traga com: bash deploy/cluster/export-platform-images.sh (de um cluster que já a tenha)" >&2
         echo "  ou, se a rede permitir: docker pull $imagem" >&2 ;;
    esac
    exit 1
  }
done
TAR="${TMPDIR:-/tmp}/codeflix-imagens.tar"

echo "salvando ${#IMAGENS[@]} imagens em $TAR"
docker save -o "$TAR" "${IMAGENS[@]}"
echo "  $(du -h "$TAR" | cut -f1)"

falhou=0
for no in $(docker ps --format '{{.Names}}' --filter "label=k3d.cluster=${CLUSTER}"); do
  case "$no" in *serverlb*|*registry*) continue;; esac
  echo "-> $no"
  docker cp "$TAR" "$no:/tmp/imagens.tar"
  docker exec "$no" ctr -n k8s.io images import --no-unpack=false /tmp/imagens.tar >/dev/null
  docker exec "$no" rm -f /tmp/imagens.tar

  # **Confere imagem por imagem.** O `ctr import` já respondeu sucesso com uma imagem faltando nesta
  # máquina, e o efeito aparece bem depois: um pod agendado naquele nó fica em ImagePullBackOff
  # tentando buscar no Docker Hub uma imagem que só existe aqui. Contar o total não pega isso.
  # **Fixa as imagens.** O kubelet coleta imagem "não usada" quando o disco passa de 85%, e imagem
  # carregada à mão não tem registro de onde vir de volta: o pod seguinte morre em ImagePullBackOff
  # tentando buscá-la no Docker Hub. Aconteceu três vezes aqui, com o disco em 83%. O rótulo
  # `pinned` é o que diz ao containerd para não a remover.
  for imagem in "${IMAGENS[@]}"; do
    ref="$(docker exec "$no" ctr -n k8s.io images ls -q | grep -m1 -E "(^|/)${imagem%%:*}:" || true)"
    [ -n "$ref" ] && docker exec "$no" ctr -n k8s.io images label "$ref" io.cri-containerd.pinned=true >/dev/null 2>&1 || true
  done

  presentes="$(docker exec "$no" ctr -n k8s.io images ls -q)"
  for imagem in "${IMAGENS[@]}"; do
    # o containerd guarda com o prefixo do registro implícito
    curta="${imagem#docker.io/}"
    if ! printf '%s\n' "$presentes" | grep -qE "(^|/)${curta%%:*}:"; then
      echo "   FALTOU: $imagem"
      falhou=1
    fi
  done
  [ "$falhou" = 0 ] && echo "   todas as ${#IMAGENS[@]} presentes"
done
rm -f "$TAR"
if [ "$falhou" != 0 ]; then
  echo
  echo "alguma imagem não chegou a algum nó. Rode de novo; se insistir, o caminho é um registro" >&2
  echo "local do k3d (k3d registry create) em vez de carregamento por tarball." >&2
  exit 1
fi
