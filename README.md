# admin-codeflix

Administração do catálogo de vídeos do Codeflix. Java 25 + Spring Boot 4.1.1 + Gradle, em arquitetura limpa.

## Módulos
- `domain`: entidades, value objects e regras de negócio (Java puro).
- `application`: casos de uso, depende apenas do `domain`.
- `infrastructure`: Spring Boot (API REST, JPA, Flyway, MySQL e o armazenamento das mídias).

## Execução
Copie o `.env.example` para `.env` e preencha usuário e senha do banco, mais as credenciais do armazenamento (`STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY` e `STORAGE_RPC_SECRET`, este último gerado com `openssl rand -hex 32`) e as da fila (`AMQP_USER` e `AMQP_PASSWORD`). O `docker compose` e a aplicação leem as variáveis desse arquivo.

```bash
cp .env.example .env
echo '127.0.0.1 keycloak' | sudo tee -a /etc/hosts   # uma vez, ver "Autenticação"
./gradlew bootJar
docker compose up -d --build
```

Isso sobe o sistema inteiro, **a aplicação inclusive**, que é a forma de homolog e produção. O jar vem do host, por
isso o `bootJar` antes. A aplicação fica em http://localhost:8080 e o depurador aceita conexão em
`APP_DEBUG_PORT` (5005 por padrão) — basta anexar a IDE nessa porta.

Para o ciclo curto de escrita, `./gradlew bootRun` continua valendo: ele reinicia em segundos, e o Filebeat
colhe o log dele também. Nesse caso suba só a infraestrutura, sem o serviço da aplicação:

```bash
docker compose up -d mysql rabbitmq garage keycloak
./gradlew bootRun
```

O `docker compose` sobe quatro serviços: o MySQL, o RabbitMQ, o Keycloak e o [Garage](https://garagehq.deuxfleurs.fr), armazenamento
compatível com S3 onde ficam os arquivos das mídias. O Garage sobe com `--single-node --default-bucket`, então
ele mesmo monta o layout e cria o bucket e a chave com as credenciais do `.env`, sem nenhum comando depois da
subida. A permissão é por chave e por bucket: a chave da aplicação só enxerga o bucket dela.

### Autenticação

O Keycloak sobe com o realm `codeflix` já importado de `.keycloak/realm.json`: as cinco roles
(`CODEFLIX_ADMIN`, `CODEFLIX_CATEGORIES`, `CODEFLIX_GENRES`, `CODEFLIX_CAST_MEMBERS` e `CODEFLIX_VIDEOS`) e o
client `admin-codeflix`, que usa o fluxo de credenciais de cliente e já vem com a role de administrador. O
console fica em http://localhost:8081, com as credenciais de `KEYCLOAK_ADMIN_USER` e `KEYCLOAK_ADMIN_PASSWORD`.

O segredo do client **não** está no arquivo versionado, que traz um marcador no lugar. O Keycloak não substitui
variável de ambiente dentro do arquivo de import, então a troca é feita no container, antes de ele ler o arquivo,
com o valor de `KEYCLOAK_CLIENT_SECRET`. Para pegar um token:

```bash
curl -s -X POST http://localhost:8081/realms/codeflix/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=admin-codeflix \
  -d client_secret="$KEYCLOAK_CLIENT_SECRET"
```

O serviço não tem volume: o realm é inteiramente descrito pelo arquivo versionado, então o container é
descartável e sempre reflete o que está no git. O que for criado à mão no console se perde ao recriá-lo.

**A API exige token.** Cada agregado tem a sua role, e `CODEFLIX_ADMIN` abre tudo:

| Rota | Roles aceitas |
|---|---|
| `/categories/**` | `CODEFLIX_ADMIN`, `CODEFLIX_CATEGORIES` |
| `/genres/**` | `CODEFLIX_ADMIN`, `CODEFLIX_GENRES` |
| `/cast-members/**` | `CODEFLIX_ADMIN`, `CODEFLIX_CAST_MEMBERS` |
| `/videos/**` | `CODEFLIX_ADMIN`, `CODEFLIX_VIDEOS` |
| qualquer outra | `CODEFLIX_ADMIN` |

A documentação da API (`/v3/api-docs` e o Swagger UI) fica aberta: não expõe dado nenhum, e em produção o
springdoc já está desligado. Sem token a resposta é 401; com token válido e role insuficiente, 403.

O realm traz dois clients: `admin-codeflix`, com a role de administrador, e `categories-codeflix`, só com a de
categorias — este serve justamente para ver o 403 nas rotas dos outros agregados. As notas do realm estão em
`.keycloak/README.md`, porque o Keycloak recusa comentário dentro do arquivo de import.

**A entrada no `/etc/hosts` não é opcional.** O `iss` do token é a URL que o cliente usou para pedi-lo, e a
aplicação só aceita token cujo `iss` bate com o emissor configurado. Com a aplicação em container, ela fala com
`http://keycloak:8081`; para o Postman e o `curl` da sua máquina usarem a **mesma** URL, `keycloak` precisa
resolver no host. Medido: token pedido em `keycloak:8081` recebe 200, e o mesmo token pedido em `localhost:8081`
recebe 401. É também por isso que o Keycloak serve na mesma porta dentro e fora do container.

O emissor é configurado por `keycloak.issuer-uri` (`KEYCLOAK_ISSUER_URI` fora do `development`). O decoder é
construído do endereço das chaves, e não do emissor, de propósito: pelo emissor o Spring buscaria os metadados
na subida, e a aplicação deixaria de subir sem o Keycloak no ar.

### Mídias dos vídeos

Cada vídeo aceita cinco arquivos — `VIDEO`, `TRAILER`, `BANNER`, `THUMBNAIL` e `THUMBNAIL_HALF` — em
`POST /videos/{id}/medias/{type}` (multipart, campo `file`) e `GET /videos/{id}/medias/{type}`. Enviar de novo o
mesmo tipo troca o arquivo anterior, e apagar o vídeo leva os arquivos junto.

O arquivo enviado é lido inteiro em memória antes de ir para o armazenamento, então o teto é
`MEDIA_MAX_FILE_SIZE` (padrão 100MB). Subir daqui pede envio em fluxo, que ainda não existe.

### Codificação dos vídeos

O arquivo de áudio e vídeo nasce com status `PENDING` e é o codificador que o move. A conversa acontece por uma
fila do RabbitMQ, também no `docker compose`, com um exchange direto (`video.events`) e uma fila para cada
sentido. O broker sobe com essa topologia já criada, vinda de `.rabbitmq/definitions.json`: o exchange, as duas filas, os
dois bindings e o usuário. A aplicação também as declara na subida, de forma idempotente, então ela continua
funcionando contra um broker vazio — por exemplo o de Testcontainers, nos testes.

O usuário vem do arquivo porque, havendo definitions, o RabbitMQ **não** cria o de `RABBITMQ_DEFAULT_USER`. Como
o nome e a senha moram no `.env`, eles entram no arquivo por substituição no container, antes da subida do nó,
com a mesma mecânica usada no Keycloak — o arquivo versionado traz só marcadores.

Quando um arquivo `VIDEO` ou `TRAILER` é enviado, sai uma mensagem em `video.created.queue` (imagem não é
codificada, e não gera aviso):

```json
{ "videoId": "...", "type": "VIDEO", "filePath": "<videoId>/VIDEO", "checksum": "...",
  "occurredOn": "2026-09-29T12:00:00Z" }
```

O codificador responde em `video.encoded.queue`, em uma de três formas, escolhidas pelo campo `status`, e
devolve o mesmo `checksum` que recebeu:

```json
{ "status": "PROCESSING", "videoId": "...", "type": "VIDEO", "checksum": "..." }
{ "status": "COMPLETED",  "videoId": "...", "type": "VIDEO", "checksum": "...", "encodedPath": "encoded/duna.mp4" }
{ "status": "ERROR",      "videoId": "...", "type": "VIDEO", "checksum": "...", "message": "codec não suportado" }
```

`PROCESSING` e `COMPLETED` movem o status da mídia, e `COMPLETED` guarda onde o arquivo codificado ficou.
`ERROR` é apenas registrado no log: o domínio ainda não tem um estado de falha para a mídia. Mensagem ilegível,
ou com tipo de mídia desconhecido, é registrada e descartada em vez de voltar para a fila.

O `checksum` existe porque o endereço do arquivo não distingue um envio do outro: reenviar o mesmo tipo
sobrescreve o arquivo no mesmo lugar. Se o usuário trocar o arquivo enquanto a codificação do anterior ainda
corre, a resposta atrasada chega com o checksum antigo e é descartada — sem isso, o arquivo novo passaria a
apontar para a saída codificada do arquivo que ele substituiu. Ele serve também ao codificador, como chave
para reconhecer trabalho que já fez.

#### Entrega garantida

A aplicação não publica direto no broker. Quando um arquivo é enviado, o aviso é gravado na tabela `outbox_event`
**dentro da mesma transação** que salva o vídeo: ou os dois existem, ou nenhum. Um relay agendado
(`OUTBOX_POLL_INTERVAL`, padrão 5s) lê o que ainda não foi entregue, manda ao broker e marca a linha como
enviada; se o broker estiver fora, a linha continua pendente e a passagem seguinte tenta de novo.

O preço é a entrega **ao menos uma vez**: uma queda entre o envio e a marcação faz a mensagem sair repetida. Por
isso o consumidor é idempotente — resposta com checksum de outro envio é descartada, e resposta repetida com o
mesmo conteúdo não regrava a mídia nem mexe no `updated_at`.

A linha entregue fica guardada por `OUTBOX_RETENTION` (padrão 7 dias) e depois é apagada, porque a tabela é fila
e não histórico. Linha pendente nunca é apagada. O relay pressupõe **uma instância** da aplicação: com várias, o
passo seguinte seria travar a leitura com `SKIP LOCKED`.

**Não há codificador neste repositório.** Para ver o ciclo completo, envie um arquivo pela API e publique a
resposta à mão no painel do RabbitMQ (http://localhost:15672, com as credenciais do `.env`): na aba *Exchanges*,
escolha `video.events`, use a routing key `video.encoded` e cole um dos JSON acima.

### Perfis
| Perfil | Quando | Configuração do banco | Swagger UI |
|---|---|---|---|
| `development` | padrão, sem perfil ativo (`bootRun`, testes) | `.env`, com `localhost` como padrão | sim |
| `homolog` | ambiente de homologação | variáveis de ambiente (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) | sim |
| `production` | produção | variáveis de ambiente, todas obrigatórias | não |

O tamanho do pool de conexões vem de `DB_POOL_SIZE` (padrão 5 no `development` e 20 nos outros). O perfil é escolhido por `SPRING_PROFILES_ACTIVE`:

```bash
SPRING_PROFILES_ACTIVE=homolog java -jar build/libs/application.jar
```

Com a aplicação no ar, a documentação da API (Swagger UI) fica em http://localhost:8080/swagger-ui.html, e o JSON do OpenAPI em http://localhost:8080/v3/api-docs.

## Observabilidade

Elasticsearch, Kibana, Logstash e Filebeat ficam atrás do profile `observability`, então o `docker compose up -d`
do dia a dia continua subindo só os quatro serviços da aplicação. A pilha entra sob demanda:

```bash
docker compose --profile observability up -d
```

O Elasticsearch pede meio giga de heap, por isso não sobe junto. Uma rede à parte não é necessária: a rede padrão
do projeto já liga todos pelo nome do serviço.

### O caminho do log

O Filebeat tem duas entradas, porque a aplicação pode rodar de dois jeitos. Em container — a forma de homolog e
produção — ele a descobre pela label `filebeat_collector` e lê o log do Docker, com `filestream` mais o parser
`container`; `type: container` foi descontinuado no Filebeat 9 e é recusado. Com `bootRun`, ele colhe o arquivo
montado de `build/logs`.

No `development` a aplicação roda no host, então ela escreve o log em **ECS** — o esquema da própria Elastic — no
arquivo `build/logs/admin-codeflix.json`, e o console segue legível para quem está desenvolvendo. O Filebeat monta
essa pasta e colhe o arquivo. Em `homolog` e `production`, onde a aplicação roda em container, é o inverso: o ECS
sai no **stdout** e não há arquivo, porque ali quem recolhe é o coletor de logs do container.

Quem formata é o próprio Boot 4, por `logging.structured.format`: não há `logback-spring.xml` nem encoder a mais
no classpath.

### Os pares chave:valor

As mensagens da aplicação carregam pares no formato `[chave:valor]`, e o filtro `kv` do Logstash
(`.observability/logstash/pipeline/logstash.conf`) os transforma em campos de verdade, com o prefixo `evento_`.
Uma mensagem assim:

```
[message:video.encoded] [status:unreadable] [payload:{"status":"CANCELADO","videoId":"abc"}]
```

chega ao Elasticsearch com:

```
evento_message = video.encoded
evento_status  = unreadable
evento_payload = {"status":"CANCELADO","videoId":"abc"}
```

Repare que o `payload` sobrevive inteiro: o `kv` divide só no primeiro dois-pontos de cada par, então o JSON de
dentro não é quebrado. Com isso dá para filtrar por `evento_status` no Kibana em vez de procurar texto.

### Kibana

O Kibana fica em http://localhost:5601. Na primeira subida, crie a *data view* do índice:

```bash
curl -s -X POST http://localhost:5601/api/data_views/data_view \
  -H 'kbn-xsrf: true' -H 'content-type: application/json' \
  -d '{"data_view":{"title":"admin-codeflix-*","name":"admin-codeflix","timeFieldName":"@timestamp"}}'
```

Ela fica guardada no volume do Elasticsearch, então sobrevive a reinícios. Depois, em *Discover*, os campos
`evento_*` aparecem como qualquer outro.

## Testes
```bash
./gradlew test
```

O `./gradlew build` também gera o relatório de cobertura (JaCoCo) dos três módulos em `build/reports/jacoco/html/index.html`.

### Regressão manual no Postman

`.postman/admin-codeflix.postman_collection.json` cobre os endpoints dos quatro agregados. A primeira pasta pega
um token no Keycloak e o guarda numa variável que as demais requisições herdam, então **ela precisa rodar
primeiro**; preencha `clientSecret` com o `KEYCLOAK_CLIENT_SECRET` do seu `.env`, que não vai versionado. Essa
pasta também confere que a API responde 401 sem token. Importe no Postman e rode a coleção inteira no Collection Runner, de cima para baixo: as requisições guardam os ids que criam em variáveis, conferem status e corpo, e apagam tudo no fim, deixando o banco como estava.

O envio de mídia usa `.postman/files/duna.mp4`, versionado junto: no Collection Runner do Postman, selecione esse arquivo no campo `file` da requisição; pelo Newman, aponte a pasta com `newman run .postman/admin-codeflix.postman_collection.json --working-dir .postman`.

A ordem importa, porque a coleção é uma jornada: as categorias vêm primeiro, o gênero usa a categoria criada, e a última pasta exercita o 409 ao apagar uma categoria vinculada antes de limpar. A variável `baseUrl` aponta para `http://localhost:8080`.
