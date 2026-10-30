# Backup e restauração

Dois scripts, rodados da raiz:

```bash
bash provisioning/backup/backup.sh                 # copia para backups/<data>/
bash provisioning/backup/restore.sh backups/<data> # devolve (pede confirmação)
```

## A parte difícil não é o comando, é saber o que copiar

Dos **onze** volumes do compose, quatro guardam coisa que não volta de lugar nenhum. Os outros sete
ficam de fora **de propósito**: copiá-los daria sensação de proteção e aumentaria o que há para
restaurar errado.

| Volume | | Por quê |
|---|---|---|
| `mysql_data` | **fonte** | o catálogo: categorias, gêneros, elenco, vídeos |
| `subscription_mysql_data` | **fonte** | contas, planos, assinaturas e o histórico de cobrança |
| `keycloak_postgres_data` | **fonte** | os usuários — quem consegue entrar |
| `garage_meta` + `garage_data` | **fonte** | as mídias enviadas, que não se refazem |
| `catalog_elasticsearch_data` | derivado | é réplica; o Debezium refaz o snapshot |
| `kafka_data` | derivado | transporte do CDC, refeito junto com o snapshot |
| `encoder_postgres_data` | derivado | histórico de tentativas; o estado da mídia está no admin |
| `elasticsearch_data`, `filebeat_data` | derivado | log, com retenção própria |
| `rabbitmq_data` | trânsito | mensagem não entregue, não estado |

O `rabbitmq_data` merece a ressalva: perder a fila significa perder o que estava em voo. O que sai da
`admin-api` se refaz, porque a linha da tabela de saída só é marcada como entregue depois do envio —
mas um `video.encoded` já respondido pelo codificador e ainda na fila se perde, e a mídia fica em
`PROCESSING` até alguém reenviar o arquivo. É consequência conhecida, não coisa que o backup resolva.

## Dump lógico, e não cópia de volume

Copiar o diretório de um banco com ele no ar produz arquivo que **às vezes** restaura. O
`mysqldump --single-transaction` e o `pg_dump` tiram uma foto consistente sem parar ninguém. Para o
Garage vale o mesmo raciocínio pelo lado do S3: `aws s3 sync` lê objeto por objeto, em vez de copiar
a árvore LSM dele por baixo.

## A ordem da restauração, e por que o Keycloak para

Bancos primeiro, mídias depois: registro apontando para arquivo que ainda não voltou aparece
quebrado na API, enquanto arquivo sem registro é só espaço ocupado.

O `restore.sh` **derruba o Keycloak** antes de mexer no banco dele. Restaurar por baixo de um
Keycloak vivo o deixa com cache de um realm que já não existe, e o sintoma é token recusado sem
explicação.

## O índice do catálogo não é restaurado

Ele é réplica. Depois de restaurar, o caminho é refazer o snapshot:

```bash
docker compose --profile cdc up -d
bash provisioning/cdc/register-cdc.sh     # ou o equivalente do deploy/
```

## Conferido, e não só escrito

O ciclo inteiro foi exercitado: inseri uma categoria que **não** estava no backup, restaurei, e ela
desapareceu — de 5 categorias de volta para 4. O Keycloak voltou a responder em 3 segundos com o
realm e o `operador-de-teste` no lugar. Os dumps têm 23, 101 e 6 tabelas, e 19 objetos de mídia.

Um defeito apareceu nesse teste e está consertado: o `docker run -v` do passo da mídia **recusa
caminho relativo**, com uma mensagem que fala de "invalid characters for a local volume name" e não
de caminho. A primeira execução restaurou os três bancos e morreu ali. Os dois scripts agora
resolvem o caminho para absoluto.

## Os dois limites, ditos em vez de escondidos

- **O destino é local.** Backup na mesma máquina do dado não protege contra a perda da máquina.
  Levar para fora — S3 de verdade, outro host, o que for — é a parte que falta;
- **não há agendamento.** Rodar isto é decisão de quem lembra. Um `cron` no host ou um `CronJob` no
  cluster é o passo seguinte, e não existe.

No cluster a receita é a mesma por `kubectl exec` em vez de `docker exec`; os scripts falam com o
compose porque é o ambiente que existe hoje.
