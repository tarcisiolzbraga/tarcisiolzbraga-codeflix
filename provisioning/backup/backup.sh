#!/usr/bin/env bash
#
# Copia o que é **fonte de verdade** deste projeto. Nada mais.
#
# A parte difícil de um backup não é o comando, é saber o que copiar. Dos onze volumes do compose,
# sete são derivados ou de trânsito, e copiá-los seria teatro — dá a sensação de proteção e aumenta o
# que há para restaurar errado. A classificação está no README do provisioning; o resumo:
#
#   FONTE (aqui)                  o que se perde se sumir
#   mysql_data                    o catálogo: categorias, gêneros, elenco, vídeos
#   subscription_mysql_data       contas, planos, assinaturas e o histórico de cobrança
#   keycloak_postgres_data        os usuários — quem consegue entrar
#   garage (objetos)              as mídias enviadas, que não se refazem de lugar nenhum
#
#   DERIVADO (de propósito fora)  por que não
#   catalog_elasticsearch_data    é réplica; o Debezium refaz o snapshot
#   kafka_data                    transporte do CDC; refeito junto com o snapshot
#   encoder_postgres_data         histórico de tentativas de conversão; o estado da mídia está no admin
#   elasticsearch/filebeat_data   log, com retenção própria
#   rabbitmq_data                 mensagens em trânsito, não estado
#
# **Dump lógico, e não cópia de volume.** Copiar o diretório de um banco com ele no ar produz
# arquivo que às vezes restaura: o `mysqldump --single-transaction` e o `pg_dump` tiram uma foto
# consistente sem parar ninguém. Para o Garage vale o mesmo raciocínio pelo lado do S3 — `aws s3
# sync` lê objeto por objeto em vez de copiar a árvore LSM dele por baixo.
#
# O destino é local, e isso é um limite e não um descuido: backup que mora na mesma máquina do dado
# não protege contra a perda da máquina. Levar para fora é a parte que falta, e está dita no README.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
set -a; . ./.env; set +a

DESTINO="${1:-$RAIZ/backups/$(date -u +%Y-%m-%dT%H-%M-%SZ)}"
mkdir -p "$DESTINO"
# Absoluto pela mesma razão do restore.sh: o `docker run -v` do passo da mídia recusa relativo, e a
# mensagem dele fala de nome de volume em vez de caminho.
DESTINO="$(cd "$DESTINO" && pwd)"
echo "destino: $DESTINO"

# Falhar aqui é melhor que gerar um dump vazio: contêiner fora do ar não é "backup de nada", é
# backup que ninguém vai conferir até precisar.
exigeContainer() {
  docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null | grep -q true || {
    echo "o container '$1' não está no ar; suba a infraestrutura antes" >&2
    exit 1
  }
}

echo "==> catálogo (mysql)"
exigeContainer adm_videos_mysql
docker exec adm_videos_mysql mysqldump \
  --single-transaction --routines --triggers --events \
  -u root -p"$DB_ROOT_PASSWORD" "$DB_NAME" > "$DESTINO/admin-$DB_NAME.sql"

echo "==> assinatura (mysql)"
exigeContainer adm_videos_subscription_mysql
docker exec adm_videos_subscription_mysql mysqldump \
  --single-transaction --routines --triggers --events \
  -u root -p"$SUBSCRIPTION_DB_ROOT_PASSWORD" "$SUBSCRIPTION_DB_NAME" \
  > "$DESTINO/subscription-$SUBSCRIPTION_DB_NAME.sql"

echo "==> usuários (keycloak/postgres)"
exigeContainer adm_videos_keycloak_postgres
docker exec -e PGPASSWORD="$KEYCLOAK_DB_PASSWORD" adm_videos_keycloak_postgres \
  pg_dump -U "$KEYCLOAK_DB_USER" -d "$KEYCLOAK_DB_NAME" > "$DESTINO/keycloak-$KEYCLOAK_DB_NAME.sql"

echo "==> mídias (garage, pelo S3)"
exigeContainer adm_videos_garage
mkdir -p "$DESTINO/midias"
# A rede do compose, para alcançar o garage pelo nome; e o `--no-verify-ssl` não entra porque o
# endereço é http, não https mal configurado.
docker run --rm \
  --network "$(docker inspect adm_videos_garage -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')" \
  -e AWS_ACCESS_KEY_ID="$STORAGE_ACCESS_KEY" \
  -e AWS_SECRET_ACCESS_KEY="$STORAGE_SECRET_KEY" \
  -e AWS_DEFAULT_REGION="${STORAGE_REGION:-garage}" \
  -v "$DESTINO/midias:/saida" \
  amazon/aws-cli:latest \
  s3 sync "s3://$STORAGE_BUCKET" /saida --endpoint-url "http://garage:3900" --only-show-errors

echo
echo "pronto. O que foi copiado:"
du -sh "$DESTINO"/* 2>/dev/null | sed 's/^/  /'
echo
echo "para restaurar: bash provisioning/backup/restore.sh '$DESTINO'"
