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

### Credencial que sai: falar com o admin

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

### Token que entra: ler o catálogo

O catálogo também **exige token de quem o consome**, e valida o mesmo emissor que o admin valida.
São dois papéis:

| Papel | O que abre |
|---|---|
| `CODEFLIX_SUBSCRIBER` | as consultas: categorias, gêneros, membros de elenco, vídeos |
| `CODEFLIX_ADMIN` | tudo, inclusive a mutation de exemplo, que é a única porta de escrita |

A autorização é **por consulta**, e não por rota, porque o GraphQL é um endereço só: no nível HTTP
não haveria como distinguir ler o acervo de gravar. Sem token a resposta vem com classificação
`UNAUTHORIZED`; com token válido e papel insuficiente, `FORBIDDEN`; token de outro emissor, ou
expirado, é recusado antes com **401**, no filtro, sem chegar ao GraphQL.

São **dois papéis, e não um por agregado** como na referência do curso. A granularidade por agregado
não se sustentaria aqui: as relações saem resolvidas dentro do vídeo, então quem puder ler vídeos já
vê o nome da categoria, do gênero e do elenco — ela anunciaria uma restrição que a API não cumpre.

A segurança vale em **todos os perfis**, inclusive no `development`. A referência a desliga lá com
`@Profile("!development")`, e isso não serve aqui: o `development` é o perfil padrão, inclusive o dos
testes, então desligá-lo deixaria a autorização sem um único teste.

Para usar o GraphiQL local, pegue um token do Keycloak do compose do admin e cole o cabeçalho
`Authorization` na aba *Headers* do console:

```bash
curl -s -X POST http://localhost:8081/realms/codeflix/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=subscriber-codeflix \
  -d client_secret="$KEYCLOAK_CLIENT_SECRET" | jq -r .access_token
```

O client `subscriber-codeflix` é o do assinante; para a mutation de exemplo, use o `admin-codeflix`.
Os dois vivem no realm da raiz do monorepo, que é um só — veja `../provisioning/keycloak/README.md`.

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

### Quando a replicação falha

O evento diz **qual id mudou**; o registro completo vem da API REST do admin. Se essa chamada
falhar, a mensagem é retentada em tópicos próprios, com atraso que dobra a cada vez — 1s, 2s, 4s,
e assim por diante, até sete tentativas. São **63 segundos** de tolerância, e o número não é
arbitrário: um restart do admin leva cerca de 17 segundos, medidos. Com as quatro tentativas que
havia antes, a janela era de 7 segundos, e subir uma versão nova do admin custava replicação.

As retentativas **não bloqueiam**: cada degrau vive num tópico próprio, com o seu consumidor, então
uma mensagem presa não segura as outras.

O que esgotar as sete tentativas vai para a **fila morta**, que apenas registra em log. Isso é
deliberado: reprocessar ali o que já falhou sete vezes ao longo de um minuto falharia de novo. O
que chega nessa fila não é indisponibilidade passageira — é mensagem que não vai passar, por bug ou
por payload que o consumidor não entende.

**Nada se perde.** A mensagem continua no tópico da fila morta, pela retenção do Kafka. E recuperar
é barato por uma propriedade do desenho: o evento carrega **só o id**, e gravar a réplica é
idempotente. Então não é preciso reenviar a mensagem original — basta disparar a replicação
daquele id de novo:

| Como | Quando usar |
|---|---|
| republicar a mensagem no tópico principal | o caso comum; o payload está no log da fila morta |
| tocar a linha no admin, por exemplo com um `update` | quando é mais fácil chegar ao admin que ao broker |
| recriar o conector do Debezium, refazendo o snapshot | quando muita coisa se perdeu de uma vez |

#### O alerta

A pilha de observabilidade traz uma regra do Kibana que vigia exatamente essa linha de log, em
[`../provisioning/observability/kibana/`](../provisioning/observability/kibana/). Ela é versionada
como JSON e registrada por API, do mesmo jeito que o conector do Debezium:

```bash
curl -sS -X POST "http://localhost:${KIBANA_PORT}/api/alerting/rule" \
  -H 'kbn-xsrf: true' -H 'Content-Type: application/json' \
  --data-binary @provisioning/observability/kibana/dead-letter-alert.json
```

Ela procura nível `ERROR` com a expressão `fila morta` nos últimos 5 minutos, roda de minuto em
minuto e dispara a partir da primeira ocorrência. Sobe **sem ação associada**: aparece em
*Alerts*, no Kibana. Enviar e-mail ou mensagem exige conector configurado, e isso depende de onde a
pilha estiver rodando.

> O Kibana precisa de `KIBANA_ENCRYPTION_KEY` no `.env` para aceitar a regra. Sem ela, a API de
> alerting fica **desligada e responde 500**, sem dizer por quê — a interface abre normalmente, o
> que torna a falha difícil de ler.

## O que o catálogo serve

GraphQL em `/api/graphql`, com uma consulta por agregado, `video(id:)` para um título só, e os
filtros do acervo: termo, classificação, ano e as três relações. Toda consulta pede o papel de
assinante, e a mutation de exemplo pede o de administrador.

**Nada inativo é devolvido, em leitura alguma** — nem na listagem, nem por id, nem através de uma
relação. Um vídeo ativo com categorias inativas vem **sem essas categorias**. O vídeo tem uma regra a
mais: só é servido quando `active` **e** `published`. O `opened` é replicado e exposto, mas nunca
filtra: ele diz se o vídeo é aberto a todos ou restrito a assinante, que é questão de acesso.

O registro inativo **continua gravado** e replicado; o filtro é na leitura. Assim, reativar ou
publicar no admin faz reaparecer aqui na hora, sem recarga.

As relações saem **resolvidas**, não como lista de ids: o cliente recebe o nome em vez de um id
opaco, e id cru deixaria passar o de um registro desativado. A resolução é em lote — as relações de
toda a página saem numa consulta por tipo, não numa por item.

Toda listagem é paginada, e a **página tem teto**: `perPage` vai de 1 a 100, e pedido fora da faixa
é recusado com `BAD_REQUEST` em vez de reduzido ao máximo — quem pedisse mil itens e recebesse cem
pediria a página seguinte em cima de uma premissa errada. Paginar sem limitar o tamanho seria
devolver coleção sem limite com um nome melhor.

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

### Regressão manual

`.postman/videos-api-codeflix.postman_collection.json` é a coleção do Postman do catálogo, feita para
rodar de cima para baixo no Collection Runner. Como é uma API GraphQL, tudo é `POST` no mesmo
endereço e toda resposta vem com status 200 — quem diz que algo deu errado é o campo `errors`, e é
nele que os testes olham.

Diferente da coleção do `admin-codeflix`, esta **não cria nem apaga o que lê**: a única porta de
escrita daqui é a mutation de exemplo. As categorias são semeadas por ela, com ids fixos que cada
execução sobrescreve em vez de acumular, e gênero, membro de elenco e vídeo são só lidos — para
vê-los com conteúdo é preciso o CDC rodando. As pastas deles passam com o catálogo vazio: afirmam o
contrato, e o conteúdo quando houver.

A primeira pasta pega os dois tokens no Keycloak e os guarda em variáveis da coleção: o de assinante
vale para a coleção toda, e as requisições que gravam trocam para o de administrador. Preencha
`clientSecret` com o `KEYCLOAK_CLIENT_SECRET` do `.env` do admin.

Fora do Postman, a mesma coleção roda na linha de comando:

```bash
npx newman run .postman/videos-api-codeflix.postman_collection.json \
  --env-var clientSecret="$KEYCLOAK_CLIENT_SECRET"
```

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
| segurança no `development` | desligada por `@Profile` | ligada em todo perfil |
| papéis | um por agregado, mais assinante | assinante e administrador, só |
| papel da mutation | o mesmo das leituras | administrador |
| teto de `perPage` | não há | 1 a 100, pedido fora é recusado |
| token para chamar o admin | cinco classes e um job, à mão | `OAuth2AuthorizedClientManager` do Spring |
| cache da busca no admin | `@Cacheable` por id, com TTL | **não há**, de propósito — veja abaixo |
| resiliência | `@CircuitBreaker`, `@Bulkhead` e `@Retry` no cliente | `@RetryableTopic` e fila morta |

### Por que não há cache na busca ao admin

O curso guarda em cache o retorno de `categoryOfId` e dos irmãos dele. Aqui não, e a razão é que
esses clientes são chamados **só pelos listeners do Kafka** — ou seja, em cima da busca da fonte da
verdade, no caminho da replicação.

Com cache, duas alterações seguidas no mesmo registro quebram a réplica: a primeira busca guarda o
valor, a segunda recebe o guardado e replica a versão velha. E **expirar o cache não conserta**,
porque nada reconsulta: o listener só age quando chega evento, e o evento já passou. A réplica fica
errada até alguém mexer naquele registro de novo, por outro motivo.

O ganho também é menor do que parece: uma alteração em lote de 500 registros são 500 ids
diferentes, e cache por id não ajuda nisso. Ele só ajudaria em chamadas repetidas ao mesmo id, que
é justamente quando queremos o valor novo.

O mesmo raciocínio pesa contra o circuit breaker: ele existe para não pendurar quem espera uma
resposta, e aqui ninguém espera — a chamada está num consumidor de fila, onde demorar não machuca,
e o backoff do `@RetryableTopic` já evita a martelada.
