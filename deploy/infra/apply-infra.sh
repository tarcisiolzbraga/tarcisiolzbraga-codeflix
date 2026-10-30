#!/usr/bin/env bash
#
# A infra no cluster. Os arquivos de configuração vêm de `provisioning/`, que segue sendo a fonte
# única: copiá-los para cá daria dois arquivos divergindo em silêncio.
set -euo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

kubectl -n codeflix create configmap mysql-init \
  --from-file="$RAIZ/provisioning/mysql/init/" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n codeflix create configmap subscription-mysql-init \
  --from-file="$RAIZ/services/subscription-api/init/" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n codeflix create configmap rabbitmq-definitions \
  --from-file=definitions.json="$RAIZ/provisioning/rabbitmq/definitions.json" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n codeflix create configmap rabbitmq-conf \
  --from-file=rabbitmq.conf="$RAIZ/provisioning/rabbitmq/rabbitmq.conf" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n codeflix create configmap garage-config \
  --from-file=garage.toml="$RAIZ/provisioning/garage/garage.toml" --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f "$RAIZ/deploy/infra/databases.yaml"
kubectl apply -f "$RAIZ/deploy/infra/messaging.yaml"
kubectl apply -f "$RAIZ/deploy/infra/storage.yaml"
