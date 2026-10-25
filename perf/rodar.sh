#!/usr/bin/env bash
#
# Roda o teste de carga e guarda o que foi medido.
#
# O k6 vem em container: nada para instalar, e a versão fica escrita aqui em vez de depender do que
# existe na máquina de quem roda.
#
# **Tira foto das métricas do actuator antes e depois.** O resumo do k6 diz o que o cliente viu —
# latência e taxa de erro. O que o serviço sentiu (heap, GC, tempo por rota) está no
# /actuator/prometheus, e sem as duas fotos não dá para dizer *por que* a latência subiu.
#
#   bash perf/rodar.sh                       # contra o compose
#   CATALOGO_URL=http://catalogo.localtest.me \
#   KEYCLOAK_URL=http://keycloak.localtest.me \
#   METRICAS="kubectl -n codeflix exec deploy/videos-api -c videos-api -- wget -qO- http://localhost:9091/actuator/prometheus" \
#   bash perf/rodar.sh                       # contra o cluster
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
set -a; . ./.env; set +a

K6_IMAGEM="${K6_IMAGEM:-grafana/k6:1.4.0}"
CATALOGO_URL="${CATALOGO_URL:-http://localhost:${CATALOG_APP_PORT}}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:${KEYCLOAK_PORT}}"
# Por padrão lê as métricas de dentro do container do compose: a porta do actuator não é publicada
# no host de propósito, e é por isso que não dá para simplesmente apontar um curl para ela.
METRICAS="${METRICAS:-docker compose exec -T videos-api wget -qO- http://localhost:9091/actuator/prometheus}"
SAIDA="${SAIDA:-$RAIZ/perf/resultados/$(date -u +%Y-%m-%dT%H-%M-%SZ)}"
mkdir -p "$SAIDA"

echo "catálogo:  $CATALOGO_URL"
echo "resultado: $SAIDA"
echo

echo "==> métricas antes"
$METRICAS > "$SAIDA/metricas-antes.txt" 2>/dev/null || echo "   (não consegui ler o actuator; segue sem a foto)"

echo "==> k6"
# --network host para o container do k6 alcançar o que está publicado no host; com rede própria,
# `localhost` dentro dele seria ele mesmo.
# `--user` com o uid de quem chama: a imagem do k6 roda como usuário sem privilégio, e sem isto o
# `--summary-export` falha com "permission denied" no diretório montado. O erro não derruba a
# execução — ela roda, mede e descarta o resumo —, e por isso é fácil não notar.
docker run --rm -i --network host \
  --user "$(id -u):$(id -g)" \
  -e CATALOGO_URL="$CATALOGO_URL" \
  -e KEYCLOAK_URL="$KEYCLOAK_URL" \
  -e KEYCLOAK_REALM="$KEYCLOAK_REALM" \
  -e KEYCLOAK_CLIENT_SECRET="$KEYCLOAK_CLIENT_SECRET" \
  -e POR_PAGINA="${POR_PAGINA:-10}" \
  -e PAGINAS="${PAGINAS:-10}" \
  -v "$SAIDA:/saida" \
  "$K6_IMAGEM" run --summary-export=/saida/resumo.json - < perf/catalogo-leitura.js \
  | tee "$SAIDA/k6.txt"

echo "==> métricas depois"
$METRICAS > "$SAIDA/metricas-depois.txt" 2>/dev/null || true

if [ -s "$SAIDA/metricas-antes.txt" ] && [ -s "$SAIDA/metricas-depois.txt" ]; then
  echo
  echo "=== o que o serviço sentiu ==="
  python3 perf/comparar.py "$SAIDA/metricas-antes.txt" "$SAIDA/metricas-depois.txt"
fi

echo
echo "guardado em $SAIDA"
