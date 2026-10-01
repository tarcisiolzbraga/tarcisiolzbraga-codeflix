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
docker compose up -d
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

**A API ainda não exige token.** O resource server — validação do JWT e as roles por rota — entra em seguida;
por ora o Keycloak existe para emitir e inspecionar token.

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

## Testes
```bash
./gradlew test
```

O `./gradlew build` também gera o relatório de cobertura (JaCoCo) dos três módulos em `build/reports/jacoco/html/index.html`.

### Regressão manual no Postman

`.postman/admin-codeflix.postman_collection.json` cobre os endpoints dos quatro agregados. Importe no Postman e rode a coleção inteira no Collection Runner, de cima para baixo: as requisições guardam os ids que criam em variáveis, conferem status e corpo, e apagam tudo no fim, deixando o banco como estava.

O envio de mídia usa `.postman/files/duna.mp4`, versionado junto: no Collection Runner do Postman, selecione esse arquivo no campo `file` da requisição; pelo Newman, aponte a pasta com `newman run .postman/admin-codeflix.postman_collection.json --working-dir .postman`.

A ordem importa, porque a coleção é uma jornada: as categorias vêm primeiro, o gênero usa a categoria criada, e a última pasta exercita o 409 ao apagar uma categoria vinculada antes de limpar. A variável `baseUrl` aponta para `http://localhost:8080`.
