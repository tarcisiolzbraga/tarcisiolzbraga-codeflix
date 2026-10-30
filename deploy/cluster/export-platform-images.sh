#!/usr/bin/env bash
#
# Tira as imagens de plataforma (Istio, Kiali, Prometheus, Grafana, Jaeger, metrics-server) de dentro
# do cluster e as guarda no Docker do host.
#
# **Por que isto existe:** nesta rede os nós não têm saída TCP para a internet — ICMP passa, 443 não
# (medido). As imagens só chegaram no primeiro `istioctl install`, quando a saída ainda funcionava.
# Recriar o cluster sem elas no host deixaria a malha sem subir, e aí o conserto dependeria de
# liberar a rede ou reiniciar o daemon do Docker com sudo.
#
# Com elas no host, `push-images.sh` as empurra para o registro de qualquer cluster novo — e a recriação passa a não
# depender de ninguém.
#
# `--platform linux/amd64`, e **não** `--all-platforms`: o containerd do nó guarda só a plataforma que
# roda, e pedir todas falha com "content digest ... not found" — o mesmo erro que derruba o
# `k3d image import --mode direct`.
set -euo pipefail

CLUSTER="${CLUSTER:-codeflix}"
PLATAFORMA="${PLATAFORMA:-linux/amd64}"
PADRAO='istio|kiali|prom|grafana|jaeger|metrics-server'

nos=()
for no in $(docker ps --format '{{.Names}}' --filter "label=k3d.cluster=${CLUSTER}"); do
  case "$no" in *serverlb*|*registry*) continue;; esac
  nos+=("$no")
done
[ ${#nos[@]} -gt 0 ] || { echo "nenhum nó do cluster '$CLUSTER' em execução" >&2; exit 1; }

# A lista é a união do que cada nó tem: o Istio espalha os componentes, e nenhum nó tem tudo.
imagens="$(for no in "${nos[@]}"; do
  docker exec "$no" ctr -n k8s.io images ls -q | grep -E "$PADRAO" | grep -v sha256 || true
done | sort -u)"

for imagem in $imagens; do
  for no in "${nos[@]}"; do
    docker exec "$no" ctr -n k8s.io images ls -q | grep -qx "$imagem" || continue
    docker exec "$no" ctr -n k8s.io images export --platform "$PLATAFORMA" /tmp/export.tar "$imagem" >/dev/null 2>&1 || continue
    docker cp "$no:/tmp/export.tar" /tmp/plataforma.tar >/dev/null
    docker exec "$no" rm -f /tmp/export.tar
    # O `docker load` de um export do containerd traz a imagem sem nome; a etiqueta vem do digest.
    carregada="$(docker load -i /tmp/plataforma.tar 2>/dev/null | grep -oE 'sha256:[0-9a-f]+|Loaded image.*' | tail -1)"
    case "$carregada" in
      sha256:*) docker tag "$carregada" "$imagem" && echo "  no host: $imagem" ;;
      *) echo "  no host: $imagem ($carregada)" ;;
    esac
    rm -f /tmp/plataforma.tar
    break
  done
done
