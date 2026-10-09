# codeflix

Os serviços de back-end do Codeflix, num repositório só. Cada serviço tem o seu build, o seu
`Dockerfile` e o seu README; o que é de todos fica na raiz.

| Pasta | O que é | Porta |
|---|---|---|
| [`admin-api/`](admin-api/) | administra o catálogo: cria, edita e publica. É a fonte do dado. | 8080 |
| [`videos-api/`](videos-api/) | serve o catálogo ao usuário final, replicando o que a `admin-api` publica. | 8082 |
| [`encoder-api/`](encoder-api/) | converte as mídias que a `admin-api` recebe. Em Go. | — |
| [`build-logic/`](build-logic/) | não é serviço: a configuração de build que os serviços Java dividem. | — |
| [`provisioning/`](provisioning/) | não é serviço: os containers de apoio, um compose por parte. | — |

A leitura de cada um está no README da própria pasta. Comece pelo
[`admin-api/README.md`](admin-api/README.md) se quer entender de onde o dado vem, ou pelo
[`videos-api/README.md`](videos-api/README.md) se quer entender como ele é servido.

## Por que num repositório só

Os serviços não são independentes, e tratá-los como tal custava caro:

- **O realm é um só.** Dar um papel ao assinante do catálogo era mudança em dois repositórios, e a
  `videos-api` mantinha uma cópia mínima do realm só para os testes dela, que podia divergir da de
  verdade. Hoje [`provisioning/keycloak/realm.json`](provisioning/keycloak/) é o único, e os
  testes dos dois leem dele.
- **O contrato do CDC atravessa os dois.** O nome do tópico está na configuração da `videos-api`, o
  conector está na `admin-api`, e as mensagens de teste da `videos-api` foram capturadas da saída
  real da `admin-api`. Renomear uma tabela de um lado quebra o outro, e agora um push só roda as
  duas suítes.
- **A infraestrutura é compartilhada.** MySQL, Keycloak, Kafka e RabbitMQ servem aos dois.
- **O build é o mesmo.** Mesma toolchain, mesmo JUnit, mesmas tasks de teste. Eram dois `buildSrc`
  iguais que já tinham começado a divergir; hoje são um [`build-logic/`](build-logic/) só, que cada
  serviço inclui pelo `settings.gradle`.

## Como rodar

A infraestrutura é um projeto só, com um `.env` só. O [`docker-compose.yml`](docker-compose.yml) da
raiz não declara serviço nenhum: ele inclui o compose de cada parte, e cada container mora na
própria pasta, junto dos arquivos que monta.

```bash
cp .env.example .env                 # primeira vez: preencher os segredos
docker compose up -d mysql rabbitmq garage keycloak catalog-elasticsearch
docker compose --profile cdc up -d   # Kafka e Kafka Connect, para a replicação
```

Os comandos são os de sempre, rodados da raiz: a inclusão é montagem de arquivo, não projeto
separado, então `up`, `down`, `logs` e os profiles enxergam tudo junto.

```
provisioning/
├── mysql/                  banco da admin-api, e a origem do binlog
├── garage/                 armazenamento das mídias, compatível com S3
├── rabbitmq/               fila da conversa com o codificador
├── keycloak/               autenticação dos dois, com um realm só
├── catalog-elasticsearch/  índice de leitura do catálogo
├── cdc/                    Kafka e Kafka Connect, mais o conector do Debezium
└── observability/          Elasticsearch de log, Kibana, Logstash e Filebeat
```

Cada serviço compila e roda a partir da própria pasta, e é de lá que o Gradle roda:

```bash
cd admin-api && ./gradlew bootRun    # ou videos-api
```

O serviço continua sendo o build de entrada; `build-logic/` entra como build incluído, compilado
antes dele. Não há wrapper na raiz, e não se roda Gradle de lá.

Para subir as aplicações em container, empacote o jar antes e depois use o compose:

```bash
cd admin-api && ./gradlew bootJar && cd ..
docker compose up -d --build admin-api
```

O `.env` único é mais do que arrumação: `KEYCLOAK_HOST` agora **não pode** divergir entre os dois
lados, e era a divergência dele que fazia o admin recusar o token do catálogo pelo `iss`.

> **Migração, uma vez só.** Quem tinha os dois composes antigos em pé precisa derrubar o projeto
> antigo do catálogo (`docker compose -p videos-api-codeflix down`) antes do primeiro `up` daqui: o
> container do Elasticsearch tem o mesmo nome nos dois. O índice replicado nasce vazio no volume
> novo, e o Debezium refaz o snapshot — é réplica, não fonte.

## Integração contínua

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) é um workflow só, com os dois serviços Java
numa matriz: testes sem container, build completo com Testcontainers, e a imagem Docker de cada um.

**Não há filtro por caminho, de propósito.** Filtrar teste pelo que mudou daria push verde com
contrato quebrado: mexer só na `admin-api` quebra a `videos-api`, que replica o dado dela. O filtro
entra no dia em que houver deploy — e aí no deploy, não nos testes.

## Histórico

Os dois serviços vinham de repositórios separados, e a junção **não** reescreveu o histórico: os 364
commits dos dois lados entraram intactos, com as mensagens, as datas e as assinaturas que já tinham.
Como o `git log` ordena por data, o histórico aparece entrelaçado — a `admin-api` começou em 17/09 e
a `videos-api` em 02/10, e os dias em que as duas andaram juntas se leem como um dia só de trabalho.
