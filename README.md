# videos-api-codeflix

API de catálogo de vídeos do Codeflix, o lado que serve o usuário final. Ela **não governa dado algum**:
replica o que o `admin-codeflix` publica e o entrega ao cliente por GraphQL.

Java 25, Spring Boot 4.1.1, Gradle 9.7.1 com Groovy DSL. Pacote base `com.tarcisiolzbraga.codeflix.videos`.

## Módulos

Três módulos Gradle, com as dependências apontando só para dentro:

- `domain`: Java puro, sem dependência alguma além do JUnit nos testes.
- `application`: casos de uso; só `domain`. Também sem Spring.
- `infrastructure`: único módulo Spring Boot. Depende dos outros dois.

As entidades daqui são **réplicas imutáveis de leitura**: só se reconstroem por `with(...)`, não têm
`newX(...)` nem método que mude estado. Mudança chega como dado novo, não como chamada. Por isso não
existe `AggregateRoot` neste projeto — não há estado comum a mudar nem evento a registrar.

## Execução

```bash
cp .env.example .env                 # primeira vez: preencher KEYCLOAK_CLIENT_SECRET
docker compose up -d                 # Elasticsearch 9.5.4 (container videos_api_elasticsearch)
./gradlew bootRun                    # sobe a API em http://localhost:8082/api
```

O console do GraphQL fica em **http://localhost:8082/api/graphiql**, e a aba *Docs* dele navega o
schema inteiro. A porta é 8082 porque a 8080 é a do `admin-codeflix` e a 8081 é a do Keycloak dele.

Para o dado chegar, além disto é preciso o `admin-codeflix` de pé, com o perfil de CDC:

```bash
cd ../admin-codeflix
docker compose up -d                        # MySQL, e a aplicação se for usar a API dele
docker compose --profile cdc up -d          # Kafka e Kafka Connect
# e registrar o conector do Debezium — veja o README de lá
```

### Credencial

Esta API lê a API do `admin-codeflix`, que exige token. A credencial é o client
**`videos-api-codeflix`** no realm `codeflix`, com as quatro roles de leitura e nenhuma de escrita.
O segredo é o mesmo `KEYCLOAK_CLIENT_SECRET` do `.env` do admin.

**Não use o `categories-codeflix`**: ele existe no realm de lá para provar o 403 nas rotas dos outros
agregados, e tomá-lo emprestado o faria perder essa função — e recebe 403 em `/cast-members/**`.

O `iss` do token é a URL por onde ele foi pedido, e o admin só aceita a que ele espera, então
`KEYCLOAK_HOST` tem de ser o mesmo nos dois `.env`. Em desenvolvimento é `localhost`, porque as
aplicações rodam no host e o nome `keycloak` só resolve dentro da rede do Docker. Para rodar o admin
em container, os dois voltam para `keycloak` e a máquina precisa de `127.0.0.1 keycloak` no
`/etc/hosts`.

### Perfis

Três perfis, como no `admin-codeflix`. Sem perfil ativo — `bootRun` e os testes — vale o
**`development`**; nos outros ambientes o perfil vem de `SPRING_PROFILES_ACTIVE`.

| | `development` | `homolog` | `production` |
| --- | --- | --- | --- |
| de onde vêm os valores | `.env` da raiz | ambiente | ambiente |
| GraphiQL | no ar | no ar | fora |
| introspecção do schema | ligada | ligada | **desligada** |
| log estruturado | arquivo em ECS, console legível | stdout em ECS | stdout em ECS |
| nível do pacote da aplicação | `debug` | `info` | `info` |

Em produção o GraphiQL sai do ar **junto com a introspecção**. Desligar só o console não esconderia
nada: ele é uma página como outra qualquer, e quem quisesse o contrato pediria a introspecção direto
no `/graphql`, que é exatamente como o console a descobre.

O `application.yml` comum exige quatro variáveis **sem valor padrão**, e é o `development` que
devolve os endereços locais:

| variável | o que é | padrão no `development` |
| --- | --- | --- |
| `ELASTIC_URIS` | o Elasticsearch do catálogo | `http://localhost:9201` |
| `KAFKA_BOOTSTRAP_SERVERS` | o broker, que sobe no compose do admin | `localhost:29092` |
| `ADMIN_API_BASE_URL` | a API de onde vem o registro completo | `http://localhost:8080` |
| `KEYCLOAK_TOKEN_URI` | o endpoint de token do realm | montado de `KEYCLOAK_HOST`, `KEYCLOAK_PORT` e `KEYCLOAK_REALM` |

Ficarem sem padrão é deliberado, e o motivo é específico de um serviço de leitura: apontar para um
Elasticsearch que não existe não produz erro visível, produz **catálogo vazio** — e catálogo vazio é
indistinguível de catálogo que ainda não replicou. Fora do desenvolvimento, variável faltando
derruba a subida, que é o sintoma que se quer.

O token entra como URL inteira, e não montada a partir de host e porta, porque fora do
desenvolvimento o Keycloak atende em `https`, atrás de domínio próprio e sem porta explícita.

## Como o dado chega

```
admin MySQL (binlog) ──Debezium/Kafka Connect──▶ Kafka ──▶ listener ──▶ Elasticsearch
                                                              │              ▲
                                                              └── REST ──────┘
                                                              (registro completo)
```

O Debezium se registra como réplica do MySQL do admin e lê o binlog, então **gravar no banco de lá já
é publicar**: nada no código do admin sabe que esta API existe. O que chega no tópico é a **linha da
tabela**, não um evento de domínio, e dela só o `id` é lido — o registro completo vem da API REST do
admin. Essa segunda chamada não é cerimônia:

- as tabelas de junção (`genre_category`, `video_category`, `video_genre`, `video_cast_member`) **não
  são capturadas** pelo conector, então os vínculos existem só na resposta REST;
- a linha do `video` traz apenas chaves estrangeiras de mídia, nenhum endereço de arquivo.

Mudar um vínculo no admin chega aqui porque os agregados de lá chamam `refreshUpdatedAt()` ao mexer
nas relações, o que toca a linha e gera o evento. **É uma dependência invisível:** se algum dia um
vínculo mudar sem tocar a linha, este catálogo fica velho em silêncio.

Três coisas que o consumidor trata, todas medidas contra o ambiente de verdade:

- o snapshot inicial usa `"op": "r"`, não `c`, e é assim que o catálogo nasce povoado;
- um `DELETE` gera **duas** mensagens, o evento `d` e um registro de valor nulo (*tombstone*) para
  compactação de log, que é ignorado;
- operação desconhecida e `TRUNCATE` são registradas e ignoradas, em vez de parar a partição.

## O que o catálogo serve

GraphQL em `/api/graphql`, com uma consulta por agregado, `video(id:)` para um título só, e os
filtros do acervo: termo, classificação, ano e as três relações.

**Nada inativo é devolvido, em leitura alguma** — nem na listagem, nem por id, nem através de uma
relação. Um vídeo ativo com categorias inativas vem **sem essas categorias**. O vídeo tem uma regra a
mais: só é servido quando `active` **e** `published`. O `opened` é replicado e exposto, mas nunca
filtra: ele diz se o vídeo é aberto a todos ou restrito a assinante, que é questão de acesso.

O registro inativo **continua gravado** e replicado; o filtro é na leitura. Assim, reativar ou
publicar no admin faz reaparecer aqui na hora, sem recarga.

As relações saem **resolvidas**, não como lista de ids: o cliente recebe o nome em vez de um id
opaco, e id cru deixaria passar o de um registro desativado. A resolução é em lote — as relações de
toda a página saem numa consulta por tipo, não numa por item.

O `rating` é `String`, e não enum, por um impedimento do GraphQL: os rótulos do admin são `ER`, `L`,
`10`, `12`, `14`, `16` e `18`, e valor de enum GraphQL não pode começar com dígito. O filtro valida o
rótulo e **recusa o desconhecido** com `BAD_REQUEST`, em vez de ignorá-lo e devolver o acervo inteiro.

## Testes

```bash
./gradlew build                      # tudo; porta de entrada de cada commit
./gradlew unitTests                  # só os que não sobem container (dispensa Docker)
./gradlew integrationTests           # só @IntegrationTest
./gradlew e2eTests                   # só @E2ETest
./gradlew :domain:test --tests '*VideoTest'   # uma classe
```

Quem precisa de Docker é marcado por tag na própria anotação, nunca classe por classe, e `unitTests`
é por exclusão — teste novo sem tag entra nela sozinho.

Quatro níveis, cada um com um papel:

| Nível | Cobre | Simula |
|---|---|---|
| unitário | domínio e casos de uso | gateway, com Mockito |
| fatia `@GraphQlTest` | o contrato do schema | os casos de uso |
| `@IntegrationTest` | gateway e listener, com Elasticsearch e Kafka reais | o cliente REST do admin |
| `@E2ETest` | a jornada por HTTP, com Tomcat | só os clientes da API do admin |

**As mensagens de CDC dos testes não são inventadas:** foram capturadas dos tópicos do
`admin-codeflix`, geradas pelo Debezium em cima do MySQL dele, e estão em
`infrastructure/src/test/resources/cdc`. Cada teste troca só o `id`, para nenhum ver mensagem de
outro.

Nos testes que esperam a chegada de uma mensagem, a presença é afirmada **antes** de ler o valor:
`getFirst()` numa lista vazia e `orElseThrow()` num `Optional` vazio lançam exceção que não é
`AssertionError`, e o Awaitility não a repetiria — o teste falharia na primeira tentativa em vez de
esperar.

## Referência

[FC3-api-de-videos-java](https://github.com/devfullcycle/FC3-api-de-videos-java), do curso FullCycle
FC3, consultado lendo o repositório. Ele **informa, não decide**, e as divergências deliberadas são:

| | Curso | Aqui |
|---|---|---|
| campo `active` | não existe nas réplicas | existe e filtra toda leitura |
| relações no schema | ids crus e resolvidos | só resolvidos |
| listagem | lista crua | página com `meta` |
| mutations | uma por agregado | uma só, de exemplo, na categoria |
| `presenter` | classe de mapeamento | fábricas `from(...)` nos próprios records |
| parâmetros | 18 na fábrica do `Video` | agrupados em value objects |
