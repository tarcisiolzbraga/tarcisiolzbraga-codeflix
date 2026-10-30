# provisioning

Os containers de apoio: o que as aplicações precisam para rodar e não é aplicação.

| Pasta | O que sobe | Profile |
|---|---|---|
| [`mysql/`](mysql/) | o banco da `admin-api`, que é a fonte do catálogo — e a origem do binlog que alimenta a replicação | — |
| [`garage/`](garage/) | o armazenamento das mídias, compatível com S3 | — |
| [`rabbitmq/`](rabbitmq/) | a fila entre as aplicações: conversão de mídia e envio de e-mail | — |
| [`keycloak/`](keycloak/) | a autenticação de todos, com **um** realm, mais o Postgres dele | — |
| [`catalog-elasticsearch/`](catalog-elasticsearch/) | o índice de leitura do catálogo | — |
| [`cdc/`](cdc/) | Kafka e Kafka Connect, com os dois conectores do Debezium | `cdc` |
| [`observability/`](observability/) | a pilha de log: Elasticsearch, Kibana, Logstash e Filebeat | `observability` |
| [`backup/`](backup/) | não sobe container: os scripts de cópia e restauração | — |

## A regra de onde cada coisa mora

Não é "toda infraestrutura aqui", e saber a diferença economiza procura:

- **o que serve a mais de uma aplicação** fica nesta pasta — o MySQL do admin, o Keycloak, a fila, o
  armazenamento, o Kafka;
- **o banco que é só de um serviço** mora **com o serviço**, junto do `Dockerfile` dele:
  `subscription-mysql` em [`services/subscription-api/`](../services/subscription-api/),
  `encoder-postgres` em [`services/encoder-api/`](../services/encoder-api/) e o coletor de e-mail em
  [`services/mail-api/`](../services/mail-api/).

Dentro de cada pasta daqui, o container mora junto dos arquivos que ele monta — o `realm.json` do
Keycloak, o `definitions.json` do RabbitMQ, o `garage.toml` — para que mexer num container seja
abrir uma pasta só.

E o agrupamento é por **função**, não um arquivo por container: `cdc` sobe Kafka e Kafka Connect, que
só existem juntos, e `observability` sobe os quatro da pilha de log.

## Como isto é montado

Por inclusão, em dois níveis. O compose da raiz não declara serviço nenhum: ele inclui o
[`docker-compose.yml`](docker-compose.yml) desta pasta, que por sua vez inclui o de cada parte. O
`.env` da raiz vale para todos — o compose o lê uma vez e interpola nos incluídos.

Então os comandos são os de sempre, rodados **da raiz**, e enxergam tudo junto:

```bash
docker compose up -d mysql rabbitmq garage keycloak catalog-elasticsearch
docker compose --profile cdc up -d              # Kafka e Kafka Connect
docker compose --profile observability up -d    # a pilha de log
```

## O que se perde ao apagar um volume, e o que não

Quatro volumes são **fonte de verdade** e não se refazem: o MySQL do admin, o MySQL da assinatura, o
Postgres do Keycloak e os dois do Garage. Todo o resto é derivado ou trânsito — o índice do catálogo
é réplica que o Debezium reconstrói, o Kafka é transporte, o log tem retenção própria.

A conta completa, com o porquê de cada um, está em [`backup/README.md`](backup/).

## As duas pastas com README próprio

[`keycloak/`](keycloak/) e [`backup/`](backup/) têm o seu, e as outras seis não — de propósito. Um
compose com a configuração ao lado se lê sozinho; essas duas carregam coisa que não se lê no
arquivo: um **procedimento** (como copiar e restaurar, e o que deliberadamente não se copia) e uma
**decisão** com preço (por que o Keycloak ganhou banco, e o que isso custou ao reimportar o realm).

README novo aqui quando houver desse tipo de coisa a dizer — não para repetir o que o compose já diz.
