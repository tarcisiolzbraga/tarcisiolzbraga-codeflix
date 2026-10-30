#!/usr/bin/env bash
#
# Mede um nível de carga por vez e monta a tabela.
#
# É a diferença entre "aguentou 80 VUs" e "a partir de 20 a latência deixa de ser plana". A rampa
# dentro de uma execução só dá um p99 do conjunto, que não responde nem a primeira pergunta nem a
# segunda; aqui cada nível é uma execução, com aquecimento próprio.
#
#   bash perf/degraus.sh                  # 1 5 10 20 40 80
#   NIVEIS="1 10 100" bash perf/degraus.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
set -a; . ./.env; set +a

K6_IMAGEM="${K6_IMAGEM:-grafana/k6:1.4.0}"
CATALOGO_URL="${CATALOGO_URL:-http://localhost:${CATALOG_APP_PORT}}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:${KEYCLOAK_PORT}}"
NIVEIS="${NIVEIS:-1 5 10 20 40 80}"
DURACAO="${DURACAO:-40s}"
SAIDA="${SAIDA:-$RAIZ/perf/resultados/degraus-$(date -u +%Y-%m-%dT%H-%M-%SZ)}"
mkdir -p "$SAIDA"

echo "catálogo: $CATALOGO_URL | níveis: $NIVEIS | $DURACAO cada | forma: ${FORMA:-acervo}"
echo

for vus in $NIVEIS; do
  echo "==> $vus VU(s)"
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
    -e PAGINAS="${PAGINAS:-20}" \
    -e VUS="$vus" \
    -e FORMA="${FORMA:-acervo}" \
    -e DURACAO="$DURACAO" \
    -v "$SAIDA:/saida" \
    "$K6_IMAGEM" run --quiet --summary-export="/saida/nivel-$vus.json" - < perf/catalogo-leitura.js \
    > "$SAIDA/nivel-$vus.txt" 2>&1 || echo "   (o k6 saiu com erro; veja nivel-$vus.txt)"
done

echo
python3 perf/tabela.py "$SAIDA"
echo
echo "guardado em $SAIDA"
