#!/usr/bin/env bash
#
# Registra os dois conectores do Debezium na API do Kafka Connect.
#
# O conector não mora em arquivo da aplicação: ele é registrado nessa API, com a configuração
# versionada em `provisioning/cdc/`. Num cluster novo o Kafka nasce vazio, então **sem este passo o
# catálogo fica permanentemente vazio** — com o conector ausente e tudo o mais saudável. Já perdi
# tempo com isso.
#
# O usuário e a senha do binlog entram aqui, do `.env`, pelo mesmo motivo de sempre: não versionados.
set -euo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
set -a; . "$RAIZ/.env"; set +a

PORTA="${PORTA_CONNECT:-8083}"
# Pelo port-forward porque o Connect não está atrás do gateway: ele é ferramenta de operação, não
# parte do produto.
kubectl -n codeflix port-forward svc/kafka-connect "${PORTA}:8083" >/tmp/connect-pf.log 2>&1 &
PF=$!
trap 'kill $PF 2>/dev/null || true' EXIT
for _ in $(seq 1 30); do
  curl -sf "http://localhost:${PORTA}/connectors" >/dev/null 2>&1 && break
  sleep 2
done

for conector in admin subscription; do
  python3 -c "
import json, os
cfg = json.load(open('$RAIZ/provisioning/cdc/${conector}-mysql-cdc.json'))
cfg['database.user'] = os.environ['CDC_USER']
cfg['database.password'] = os.environ['CDC_PASSWORD']
cfg.pop('name', None)
print(json.dumps(cfg))" \
  | curl -sS -X PUT "http://localhost:${PORTA}/connectors/${conector}-mysql-cdc/config" \
      -H 'Content-Type: application/json' --data-binary @- -o /dev/null -w "  ${conector}-mysql-cdc: %{http_code}\n"
done

sleep 8
for conector in admin subscription; do
  curl -s "http://localhost:${PORTA}/connectors/${conector}-mysql-cdc/status" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(f\"  {d['name']}: {d['connector']['state']} | tarefas: {[t['state'] for t in d.get('tasks',[])]}\")"
done
