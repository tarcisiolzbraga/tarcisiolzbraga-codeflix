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

O Keycloak sobe com o realm `codeflix` já importado de `../../provisioning/keycloak/realm.json`, na raiz do
monorepo: as seis roles
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

O realm traz quatro clients: `admin-codeflix`, com a role de administrador; `categories-codeflix`, só com a de
categorias — este serve justamente para ver o 403 nas rotas dos outros agregados; `videos-api-codeflix`, com
as quatro roles de leitura, que é a credencial da API de catálogo e nunca escreve nada aqui; e
`catalog-test-codeflix`, com a role de assinante, que **não** serve para nada aqui — é um client de teste
que lê o catálogo do outro lado, e está neste realm porque o realm é um só. As notas do realm estão em
`../../provisioning/keycloak/README.md`, porque o Keycloak recusa comentário dentro do arquivo de import.

**A entrada no `/etc/hosts` não é opcional.** O `iss` do token é a URL que o cliente usou para pedi-lo, e a
aplicação só aceita token cujo `iss` bate com o emissor configurado. Com a aplicação em container, ela fala com
`http://keycloak:8081`; para o Postman e o `curl` da sua máquina usarem a **mesma** URL, `keycloak` precisa
resolver no host. Medido: token pedido em `keycloak:8081` recebe 200, e o mesmo token pedido em `localhost:8081`
recebe 401. É também por isso que o Keycloak serve na mesma porta dentro e fora do container.

O emissor é configurado por `keycloak.issuer-uri` (`KEYCLOAK_ISSUER_URI` fora do `development`). O decoder é
construído do endereço das chaves, e não do emissor, de propósito: pelo emissor o Spring buscaria os metadados
na subida, e a aplicação deixaria de subir sem o Keycloak no ar.

### O front noutra origem

Requisição de browser vinda de outra origem é aceita por perfil: `localhost` no `development`,
`h.tarcisiolzbraga.codeflix.com` na homologação e `tarcisiolzbraga.codeflix.com` na produção, sobrescrevendo com
`CORS_ALLOWED_ORIGINS`. Não é segurança desta API — quem autoriza é o token; o que isso muda é o
browser deixar o front **ler** a resposta.

A decisão, e por que o `localhost` está na homologação e não na produção, está no
[README da raiz](../../README.md#o-front-e-o-cors): vale igual nos três serviços.

### O teto da descrição, e um 500 que virou 422

A coluna `description` é `VARCHAR(4000)` na categoria e no vídeo. O `VideoValidator` sempre conferiu
esse teto; o `CategoryValidator` **não** — ele validava só o nome.

A consequência era medível: uma categoria com 4001 caracteres de descrição passava pelo domínio,
quebrava na coluna, e o `DataIntegrityViolationException` não está mapeado no
`GlobalExceptionHandler` — a API respondia **500**, com corpo sem explicação nenhuma. Medido contra
a API de verdade antes do conserto.

Agora o domínio recusa com `'description' must be at most 4000 characters`, e a resposta é 422 como
nos outros erros de validação. A descrição da categoria continua **opcional**, diferente da do
vídeo: nula e em branco são estados válidos, e o teto é a única regra.

A coleção do Postman tem o caso, e o `CategoryE2ETest` também.

### Mídias dos vídeos

Cada vídeo aceita cinco arquivos — `VIDEO`, `TRAILER`, `BANNER`, `THUMBNAIL` e `THUMBNAIL_HALF` — em
`POST /videos/{id}/medias/{type}` (multipart, campo `file`) e `GET /videos/{id}/medias/{type}`. Enviar de novo o
mesmo tipo troca o arquivo anterior, e apagar o vídeo leva os arquivos junto.

O arquivo enviado é lido inteiro em memória antes de ir para o armazenamento, então o teto é
`MEDIA_MAX_FILE_SIZE` (padrão 100MB). Subir daqui pede envio em fluxo, que ainda não existe.

### A mídia aceita `Range`

`GET /videos/{id}/medias/{type}` responde o arquivo inteiro com 200, e **só o trecho pedido com 206**
quando o cliente manda `Range`. É o que permite a um player buscar na linha de tempo sem baixar tudo —
e quem consome isso é o catálogo, que repassa a rota a quem assina.

**O cabeçalho não é interpretado aqui.** Ele vai como veio até o armazenamento: o S3 entende
`bytes=0-1023` e devolve o `Content-Range` pronto, com o trecho servido e o tamanho total. Refazer
essa conta na rota seria repetir o que o armazenamento já fez, e errar nas pontas.

Trecho que o arquivo não tem responde **416, e não 404**: o arquivo está lá, o intervalo é que não
existe. `Accept-Ranges: bytes` vai nas duas respostas, porque é por ele que o player descobre que
pode pedir trecho.

O caso de uso do trecho é separado do caso de uso do arquivo inteiro, de propósito: a resposta é
outra, e misturar os dois num só com parâmetro opcional faria a rota decidir o status por `if` de
campo nulo.

### O trecho sai em fluxo, e o que isso custava antes

O trecho vai do armazenamento para o socket **sem passar inteiro pela memória**. Isso não era verdade
até aqui, e o preço era contável: um pedido de 10 MB chegava a **nove** cópias do mesmo trecho entre
este serviço e o catálogo.

| onde | cópias |
|---|---|
| admin-api | `getObjectAsBytes`, `asByteArray`, construtor do `ResourceSlice`, accessor dele, construtor do `MediaSliceOutput`, accessor dele |
| catálogo | `bodyTo(byte[])`, construtor do `VideoMediaFile`, accessor dele |

Quatro delas eram **cópia defensiva** — necessária enquanto o conteúdo era um arranjo mutável dentro
de um value object, e desnecessária depois: não há bytes para copiar.

O conteúdo virou um [`MediaContent`](domain/src/main/java/com/tarcisiolzbraga/codeflix/admin/domain/video/MediaContent.java):
uma função que recebe o destino e escreve nele. É função, e não `InputStream`, porque `InputStream`
no domínio traria um recurso com ciclo de vida para um lugar que não sabe fechá-lo — aqui quem abre
fecha, dentro do `writeTo`, inclusive quando a escrita falha no meio.

O 404 e o 416 continuam chegando **na abertura** do objeto, e não durante a escrita: é isso que
permite a rota escolher o status antes de começar a responder.

**O caminho sem `Range` continua montando o corpo em memória, e é escolha.** Ele serve as artes do
catálogo — banner e miniatura —, que são pequenas, e o player sempre manda `Range`.

### O envio também, e o checksum é o nó

O envio de 100 MB chegava a três cópias do arquivo inteiro em memória: `getBytes()` na rota, e as duas
cópias defensivas do `Resource`.

O nó é o **checksum**: ele é gravado como metadado do objeto no armazenamento, então tem de ser
conhecido **antes** do PUT — não dá para calculá-lo durante o envio. A saída são duas passadas sobre
o arquivo, e por isso o conteúdo de um `Resource` é um
[`MediaSource`](domain/src/main/java/com/tarcisiolzbraga/codeflix/admin/domain/video/MediaSource.java):
uma fonte que pode ser **aberta mais de uma vez**.

As duas passadas são de graça: acima do limiar de memória, o Spring já gravou o multipart num arquivo
temporário em disco, e abrir o `getInputStream()` dele duas vezes é ler duas vezes um arquivo local. A
alternativa — guardar os bytes para precisar de uma passada só — é justamente o custo que isto remove.

O checksum sai do `DigestInputStream`, lendo em blocos. O PUT usa
`RequestBody.fromInputStream` **com o tamanho**: sem `Content-Length` o SDK bufferiza o corpo para
descobri-lo, o que anularia a troca.

O `Resource` perdeu a cópia defensiva e o `equals` por bytes, e isso é consequência: não há arranjo a
proteger, e comparar duas fontes exigiria abrir e ler as duas. Sobrou **uma** cópia, na fábrica que
recebe um arranjo — o lugar certo para ela, em vez de em toda leitura.

### Codificação dos vídeos

O arquivo de áudio e vídeo nasce com status `PENDING` e é o codificador que o move. A conversa acontece por uma
fila do RabbitMQ, também no `docker compose`, com um exchange direto (`video.events`) e uma fila para cada
sentido. O broker sobe com essa topologia já criada, vinda de `../../provisioning/rabbitmq/definitions.json`: o exchange, as duas filas, os
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

### Replicação para o catálogo

A API de catálogo (`videos-api-codeflix`) é alimentada por **change data capture**: o Debezium se registra como
réplica do MySQL e lê o binlog, então gravar no banco já é publicar. Nada no código desta aplicação sabe que isso
existe — não há evento a registrar, nem publicador, nem risco de alguém salvar uma categoria e esquecer de avisar.
Isto é independente do RabbitMQ, que serve só à conversa com o codificador.

Os dois serviços ficam atrás do profile `cdc`, porque a aplicação não depende deles:

```bash
docker compose --profile cdc up -d        # kafka e kafka-connect
```

O Kafka anuncia dois endereços: `kafka:9092` para quem está na rede do compose e `localhost:${KAFKA_PORT}` para
quem roda no host, como o `bootRun` da API de catálogo. Um listener só não serviria, porque o endereço anunciado
tem de ser alcançável por quem conecta.

O Kafka Connect é a casa onde o conector roda, com API REST na `${KAFKA_CONNECT_PORT}`. O conector não mora em
arquivo da aplicação: ele é registrado nessa API, com a configuração versionada em
`../../provisioning/cdc/admin-mysql-cdc.json`, cujos marcadores de usuário e senha são trocados na hora pelos valores do `.env`:

```bash
CU=$(grep '^CDC_USER=' .env | cut -d= -f2-); CP=$(grep '^CDC_PASSWORD=' .env | cut -d= -f2-)
python3 -c "
import json
cfg = json.load(open('provisioning/cdc/admin-mysql-cdc.json'))
cfg['database.user'] = '$CU'; cfg['database.password'] = '$CP'
print(json.dumps(cfg))" \
  | curl -sS -X PUT http://localhost:8083/connectors/admin-mysql-cdc/config \
      -H 'Content-Type: application/json' --data-binary @-

curl -sS http://localhost:8083/connectors/admin-mysql-cdc/status   # deve dizer RUNNING
```

O usuário que lê o binlog tem permissões de **replicação**, não de leitura comum, e nasce do script
`../../provisioning/mysql/init/10-cdc-user.sh`. Esse script roda uma única vez, **na criação do volume** `mysql_data`: num banco que
já existe ele não roda, e o usuário se cria à mão uma vez:

```sql
CREATE USER IF NOT EXISTS 'debezium'@'%' IDENTIFIED BY '<CDC_PASSWORD do .env>';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium'@'%';
FLUSH PRIVILEGES;
```

Sem `mysql_native_password`: o plugin vem desabilitado no MySQL 8.4, então o usuário fica com o
`caching_sha2_password` padrão, e é por isso que o conector precisa de `database.allowPublicKeyRetrieval`. O resto
da configuração do servidor não precisa de ajuste — o 8.4 já vem com `log_bin` ligado, formato `ROW` e
`binlog_row_image=FULL`.

Cada tabela capturada vira um tópico `adm_videos_mysql.adm_videos.<tabela>`, e a lista é só a das raízes
(`category`, `genre`, `cast_member`, `video`) — nada de tabelas de junção, nem das `_aud` do Envers, nem da
`outbox_event`. O tópico só nasce quando a tabela tem a primeira linha. O consumidor recebe a **linha**, não um
evento de domínio, e usa o id dela para buscar o registro completo na API REST daqui.

Três coisas que o consumidor precisa tratar, medidas contra este ambiente:

- O snapshot inicial lê as tabelas inteiras e publica cada linha existente com `"op": "r"`, não `c`. É assim que o
  catálogo nasce povoado, sem carga manual.
- Um `DELETE` gera **duas** mensagens: o evento `"op": "d"`, com a linha em `before`, e em seguida um registro de
  **valor nulo** (*tombstone*), para compactação de log. O consumidor tem de aceitar payload nulo e ignorá-lo.
- `BOOLEAN` chega como `1`/`0` (é `TINYINT(1)` no MySQL) e `DATETIME(6)` chega como microssegundos desde a época.
  Não afeta quem só lê o `id`.

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

## Integração contínua

`.github/workflows/ci.yml` roda em qualquer branch e em pull request, em três etapas encadeadas, da mais rápida
para a mais cara:

1. **Testes sem container** — `./gradlew unitTests`, que dispensa Docker e responde em segundos. Erro bobo não
   espera container subir.
2. **Build completo** — `./gradlew build`, com os testes de integração e E2E subindo MySQL, Garage, RabbitMQ e
   Keycloak em Testcontainers. O runner tem Docker, e os testes não precisam do `.env`: quem fornece credencial
   são os próprios containers. O relatório dos testes fica guardado como artefato.
3. **Imagem Docker** — gera o jar e constrói a imagem **sem publicar**. Não exige credencial, e passa a verificar
   o `Dockerfile`, que fora disso não é exercitado por nada.

O jar do wrapper é validado contra os oficiais do Gradle na primeira etapa, e um push novo na mesma referência
cancela a execução anterior.

A publicação da imagem entra quando houver registro e segredo definidos.

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
(`../../provisioning/observability/logstash/pipeline/logstash.conf`) os transforma em campos de verdade, com o prefixo `evento_`.
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
