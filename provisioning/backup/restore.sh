#!/usr/bin/env bash
#
# Restaura o que o backup.sh copiou.
#
# **Isto sobrescreve dado.** O script pede confirmação, porque restaurar por engano é a maneira mais
# rápida de transformar um problema pequeno num grande.
#
# A ordem importa: os bancos primeiro, as mídias depois. Um vídeo cujo registro aponta para um
# arquivo que ainda não voltou aparece quebrado na API; o inverso — arquivo sem registro — é só
# espaço ocupado, que a restauração do banco em seguida resolve.
#
# O que este script **não** faz: subir a infraestrutura, recriar o realm do Keycloak a partir do
# realm.json, ou refazer o índice do catálogo. Os dois últimos são de propósito — o dump do Keycloak
# já traz o realm, e o índice é réplica: `docker compose --profile cdc up -d` mais o registro do
# conector refazem o snapshot. Está no README.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
set -a; . ./.env; set +a

ORIGEM="${1:-}"
# **Caminho absoluto, sempre.** O `docker run -v` recusa caminho relativo com uma mensagem que fala
# de "invalid characters for a local volume name" e não de caminho — ele acha que você quis nomear um
# volume. Medido: a primeira execução deste script, com caminho relativo, restaurou os três bancos e
# morreu na mídia.
[ -n "$ORIGEM" ] && [ -d "$ORIGEM" ] && ORIGEM="$(cd "$ORIGEM" && pwd)" || {
  echo "uso: bash provisioning/backup/restore.sh <pasta-do-backup>" >&2
  echo "backups disponíveis:" >&2
  ls -1 "$RAIZ/backups" 2>/dev/null | sed 's/^/  /' >&2 || echo "  (nenhum)" >&2
  exit 1
}

echo "Isto vai SOBRESCREVER o catálogo, a assinatura, os usuários e as mídias"
echo "com o conteúdo de: $ORIGEM"
printf 'digite "restaurar" para seguir: '
read -r resposta
[ "$resposta" = "restaurar" ] || { echo "cancelado"; exit 1; }

exigeContainer() {
  docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null | grep -q true || {
    echo "o container '$1' não está no ar; suba a infraestrutura antes" >&2
    exit 1
  }
}

echo "==> catálogo (mysql)"
exigeContainer adm_videos_mysql
docker exec -i adm_videos_mysql mysql -u root -p"$DB_ROOT_PASSWORD" "$DB_NAME" \
  < "$ORIGEM/admin-$DB_NAME.sql"

echo "==> assinatura (mysql)"
exigeContainer adm_videos_subscription_mysql
docker exec -i adm_videos_subscription_mysql mysql -u root -p"$SUBSCRIPTION_DB_ROOT_PASSWORD" \
  "$SUBSCRIPTION_DB_NAME" < "$ORIGEM/subscription-$SUBSCRIPTION_DB_NAME.sql"

# O Keycloak tem de estar **fora do ar** aqui: restaurar o banco por baixo de um Keycloak vivo deixa
# ele com cache de um realm que já não existe, e o sintoma é token recusado sem explicação.
echo "==> usuários (keycloak/postgres)"
exigeContainer adm_videos_keycloak_postgres
docker compose stop keycloak >/dev/null 2>&1 || true
docker exec -i -e PGPASSWORD="$KEYCLOAK_DB_PASSWORD" adm_videos_keycloak_postgres \
  psql -U "$KEYCLOAK_DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $KEYCLOAK_DB_NAME WITH (FORCE);" >/dev/null
docker exec -i -e PGPASSWORD="$KEYCLOAK_DB_PASSWORD" adm_videos_keycloak_postgres \
  psql -U "$KEYCLOAK_DB_USER" -d postgres -c "CREATE DATABASE $KEYCLOAK_DB_NAME;" >/dev/null
docker exec -i -e PGPASSWORD="$KEYCLOAK_DB_PASSWORD" adm_videos_keycloak_postgres \
  psql -q -U "$KEYCLOAK_DB_USER" -d "$KEYCLOAK_DB_NAME" < "$ORIGEM/keycloak-$KEYCLOAK_DB_NAME.sql" >/dev/null
docker compose start keycloak >/dev/null 2>&1 || true

echo "==> mídias (garage, pelo S3)"
exigeContainer adm_videos_garage
docker run --rm \
  --network "$(docker inspect adm_videos_garage -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')" \
  -e AWS_ACCESS_KEY_ID="$STORAGE_ACCESS_KEY" \
  -e AWS_SECRET_ACCESS_KEY="$STORAGE_SECRET_KEY" \
  -e AWS_DEFAULT_REGION="${STORAGE_REGION:-garage}" \
  -v "$ORIGEM/midias:/entrada:ro" \
  amazon/aws-cli:latest \
  s3 sync /entrada "s3://$STORAGE_BUCKET" --endpoint-url "http://garage:3900" --only-show-errors

echo
echo "pronto."
echo "O índice do catálogo é réplica e NÃO foi restaurado: suba o profile cdc e registre o conector"
echo "para o Debezium refazer o snapshot (ver provisioning/backup/README.md)."
