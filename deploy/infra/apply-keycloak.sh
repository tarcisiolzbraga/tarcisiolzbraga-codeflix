#!/usr/bin/env bash
#
# O realm e o tema vêm de `provisioning/keycloak/`, que é a fonte única: copiá-los para cá daria dois
# arquivos divergindo em silêncio — o compose leria um, o cluster o outro.
set -euo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEMA="$RAIZ/provisioning/keycloak/themes/codeflix/login"

kubectl -n codeflix create configmap keycloak-realm \
  --from-file=realm.json="$RAIZ/provisioning/keycloak/realm.json" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n codeflix create configmap keycloak-theme \
  --from-file=theme.properties="$TEMA/theme.properties" \
  --from-file=codeflix.css="$TEMA/resources/css/codeflix.css" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f "$RAIZ/deploy/mesh/gateway.yaml"
kubectl apply -f "$RAIZ/deploy/infra/keycloak.yaml"
kubectl -n codeflix rollout status deployment/keycloak --timeout=300s
