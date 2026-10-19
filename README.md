# codeflix

Os serviços de back-end do Codeflix, num repositório só. Cada serviço tem o seu build, o seu
`Dockerfile` e o seu README; o que é de todos fica na raiz.

| Pasta | O que é | Porta |
|---|---|---|
| [`services/admin-api/`](services/admin-api/) | administra o catálogo: cria, edita e publica. É a fonte do dado. | 8080 |
| [`services/videos-api/`](services/videos-api/) | serve o catálogo ao usuário final, replicando o que a `admin-api` publica. | 8082 |
| [`services/encoder-api/`](services/encoder-api/) | converte as mídias que a `admin-api` recebe. Em Go. | — |
| [`services/subscription-api/`](services/subscription-api/) | diz quem é assinante e se a cobrança do mês passou. É quem dá direito ao catálogo. | 8084 |
| [`services/mail-api/`](services/mail-api/) | entrega o e-mail que a `subscription-api` manda, consumindo a fila. Em Go. | — |
| [`clients/portal-codeflix/`](clients/portal-codeflix/) | não é serviço: o front de quem assina e assiste. | 3000 |
| [`clients/back-office-codeflix/`](clients/back-office-codeflix/) | não é serviço: o front de quem administra o catálogo. | 3001 |
| [`build-logic/`](build-logic/) | não é serviço: a configuração de build que os serviços Java dividem. | — |
| [`provisioning/`](provisioning/) | não é serviço: os containers de apoio, um compose por parte. | — |

A leitura de cada um está no README da própria pasta. Comece pelo
[`services/admin-api/README.md`](services/admin-api/README.md) se quer entender de onde o dado vem, ou pelo
[`services/videos-api/README.md`](services/videos-api/README.md) se quer entender como ele é servido.

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
docker compose up -d mysql rabbitmq garage keycloak catalog-elasticsearch subscription-mysql
docker compose --profile cdc up -d   # Kafka e Kafka Connect, para a replicação
```

Os comandos são os de sempre, rodados da raiz: a inclusão é montagem de arquivo, não projeto
separado, então `up`, `down`, `logs` e os profiles enxergam tudo junto.

A raiz separa o que é serviço do que é cliente, e o que não é nem um nem outro:

```
services/      as cinco aplicações: três em Java, duas em Go
clients/       os fronts que falam com elas
build-logic/   a configuração de build que os serviços Java dividem
provisioning/  os containers de apoio, um compose por parte
```

Os serviços em Go não expõem rota HTTP — consomem fila —, então `services/` é o nome honesto dos
cinco; `apis/` só valeria para três.

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
cd services/admin-api && ./gradlew bootRun    # ou videos-api
```

O serviço continua sendo o build de entrada; `build-logic/` entra como build incluído, compilado
antes dele. Não há wrapper na raiz, e não se roda Gradle de lá.

Os fronts sobem do mesmo jeito, da pasta de cada um — ou em container, junto do resto, com o
`docker compose up -d` da raiz. Para programar neles, o `npm run dev`:

```bash
cd clients/portal-codeflix     && npm install && npm run dev    # http://localhost:3000
cd clients/back-office-codeflix && npm install && npm run dev   # http://localhost:3001
```

Portas diferentes porque os dois rodam juntos no desenvolvimento, e as duas estão liberadas no
client do realm. Os dois leem as portas das APIs do **mesmo `.env`** desta raiz, então não há segunda
lista de portas para manter em dia.

Para subir as aplicações em container, empacote o jar antes e depois use o compose:

```bash
cd services/admin-api && ./gradlew bootJar && cd ../..
docker compose up -d --build admin-api
```

Os dois fronts e os dois serviços em Go **não** precisam disso: eles compilam dentro da própria
imagem. Só as três aplicações Java empacotam o jar no host.

### As URLs dos fronts são decididas no build, não no run

`docker compose up -d` da raiz sobe tudo, inclusive os fronts. Mas eles têm uma diferença que importa
na hora de implantar: **as URLs das APIs são embutidas no JavaScript em tempo de build**, então
passá-las no `run` não muda nada — o valor já está no pacote que o browser baixa.

Por isso elas são `build-arg`, e não variável de ambiente do container:

| no `.env` | o que acontece |
|---|---|
| `PUBLIC_*` vazias | cada URL cai para `http://localhost:<porta>`, o padrão de desenvolvimento |
| `PUBLIC_*` preenchidas | a URL inteira é usada, e a porta é ignorada |

A primeira coluna é o que a máquina de quem programa quer: as portas das APIs são publicadas no
host, e é o **browser** que as alcança — não o container do front. A segunda é como se constrói a
imagem de homologação ou de produção.

Isso significa que **uma imagem construída para um ambiente não serve para outro**, e é o preço de um
front que não lê configuração em tempo de execução. Conferido: a mesma Dockerfile, com
`--build-arg PUBLIC_CATALOG_API_URL=https://h.tarcisiolzbraga.codeflix.com`, produz um pacote com
aquela URL; sem o argumento, produz um com `localhost:8082`.

O `.env` único é mais do que arrumação: `KEYCLOAK_HOST` agora **não pode** divergir entre os dois
lados, e era a divergência dele que fazia o admin recusar o token do catálogo pelo `iss`.

> **Migração, uma vez só.** Quem tinha os dois composes antigos em pé precisa derrubar o projeto
> antigo do catálogo (`docker compose -p videos-api-codeflix down`) antes do primeiro `up` daqui: o
> container do Elasticsearch tem o mesmo nome nos dois. O índice replicado nasce vazio no volume
> novo, e o Debezium refaz o snapshot — é réplica, não fonte.

## O front, e o CORS

Os três serviços aceitam requisição de browser vinda de outra origem. **Isso não é segurança da
API**: quem autoriza é o token de cada requisição, e recusar uma origem não impede chamada nenhuma
de fora do browser — `curl`, Postman e outro serviço ignoram CORS inteiro. O que a configuração faz
é parar de o browser **esconder** a resposta de um front legítimo.

A lista é por perfil, no `application-{perfil}.yml` de cada serviço:

| Perfil | Origens aceitas |
|---|---|
| `development` | `http://localhost:3000` |
| `homolog` | `https://h.tarcisiolzbraga.codeflix.com`, `http://localhost:3000` |
| `production` | `https://tarcisiolzbraga.codeflix.com` |

Não há implantação ainda: as duas URLs são o endereço **pretendido** de cada ambiente, não um
endereço no ar. Elas estão no arquivo para a lista ser revisável em PR, e é só trocar quando o
endereço de verdade existir.

`CORS_ALLOWED_ORIGINS` sobrescreve qualquer uma delas, separando por vírgula — e, como o `.env` é um
só, o valor vale para os três serviços de uma vez. Variável vazia **derruba a subida**, em vez de a
aplicação subir recusando o front inteiro e falhar só no browser de quem for usar.

**O `localhost` está em homologação e não está em produção**, e a diferença é deliberada: apontar um
front rodando na máquina para a API de homologação é justamente o teste local que se quer poder
fazer; em produção, a mesma linha deixaria uma página servida da máquina de qualquer pessoa ler
resposta de produção com o token de quem a abriu. Se isso for desejado algum dia, é uma origem a mais
na lista de `application-production.yml` — e aí é decisão tomada, não descuido.

**Front que chama a API pelo servidor não precisa de nada disto.** Next.js com server components,
route handlers ou um proxy fazem a requisição de servidor para servidor, onde CORS não existe. A
configuração serve a quem chama do browser.

### Duas listas, não uma

O CORS e o Keycloak decidem coisas diferentes, e as duas listas precisam estar certas:

- **`cors.allowed-origins`**, em cada serviço: de onde o browser pode **ler a resposta** da API;
- **`redirectUris` do client `front-codeflix`**, no [realm](provisioning/keycloak/realm.json): para
  onde o Keycloak pode **devolver a pessoa** depois do login. É o `webOrigins: "+"` do client que
  libera o browser a falar com o Keycloak, e ele deriva dessa mesma lista.

Esquecer a segunda dá erro na tela de login do Keycloak; esquecer a primeira dá uma chamada que
chegou, respondeu, e o front não pode ler.

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
