#!/usr/bin/env bash
#
# O `.env` da raiz vira um Secret no cluster.
#
# A mesma fonte única que o compose e as aplicações já usam — e é por isso que **nenhum valor é
# versionado aqui**: o `.env` é ignorado pelo git, e este script o lê na hora de aplicar. Manifesto
# com senha dentro seria a credencial de desenvolvimento entrando no histórico para sempre.
#
# `--dry-run=client | kubectl apply` em vez de `create`: assim rodar de novo atualiza em vez de
# falhar com "already exists", que é o que se quer quando uma variável muda.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
[ -f "$RAIZ/.env" ] || { echo "não achei $RAIZ/.env" >&2; exit 1; }

kubectl create namespace codeflix --dry-run=client -o yaml | kubectl apply -f -

kubectl -n codeflix create secret generic codeflix-env \
  --from-env-file="$RAIZ/.env" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "secret 'codeflix-env' com $(kubectl -n codeflix get secret codeflix-env -o jsonpath='{.data}' | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))') chaves"
