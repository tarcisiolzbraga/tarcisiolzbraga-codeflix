#!/bin/bash
# Cria o usuário que o Debezium usa para ler o binlog. Roda uma única vez, na criação do volume
# mysql_data, pelo entrypoint da imagem oficial.
#
# É um .sh, e não um .sql, porque o entrypoint executa script de shell e só repassa arquivo .sql
# literalmente — e a senha precisa vir do ambiente, não versionada.
#
# As permissões são de replicação, não de leitura comum: o Debezium se registra como réplica do
# MySQL. REPLICATION SLAVE é o que o deixa puxar o binlog; SELECT serve ao snapshot inicial, em que
# ele lê as tabelas inteiras para o catálogo nascer povoado.
#
# Sem mysql_native_password, que o curso usa: o plugin vem desabilitado no MySQL 8.4, então o
# usuário fica com o caching_sha2_password padrão. É por isso que o conector precisa de
# database.allowPublicKeyRetrieval.
set -euo pipefail

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER IF NOT EXISTS '${CDC_USER}'@'%' IDENTIFIED BY '${CDC_PASSWORD}';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '${CDC_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "usuário de CDC '${CDC_USER}' criado"
