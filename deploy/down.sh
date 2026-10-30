#!/usr/bin/env bash
#
# Derruba o cluster inteiro. **Leva os dados com ele**: os volumes são locais ao nó, então o filme, as
# pessoas e os planos somem — `deploy/up.sh` os semeia de novo.
#
# Não toca em: imagens do host, `.env`, nem nos volumes do docker compose.
set -euo pipefail
export PATH="$HOME/.local/bin:$PATH"
CLUSTER="${CLUSTER:-codeflix}"
k3d cluster delete "$CLUSTER"
echo "cluster '$CLUSTER' removido. As imagens seguem no host, então up.sh não depende da rede."
