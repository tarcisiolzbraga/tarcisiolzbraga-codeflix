# subscription-api

O serviço de assinaturas do Codeflix: quem é o assinante, qual plano ele escolheu e se a cobrança
daquele mês passou. É ele que decide se alguém tem direito ao catálogo — a `videos-api` só serve o
que já está publicado, e nunca pergunta nada sobre pagamento.

Java 25, Spring Boot 4.1.1, Gradle 9.7.1. Pacote base `com.tarcisiolzbraga.codeflix.subscription`,
porta **8084**.

| | |
|---|---|
| Banco | MySQL 8.4 próprio, na porta 3307 |
| Persistência | JDBC com SQL escrito à mão, sem ORM |
| Autenticação | o mesmo realm do Keycloak dos outros serviços |
| Eventos | fila de saída no banco, lida pelo Debezium |

## O ciclo de vida de uma assinatura

```mermaid
flowchart TD
    A[Criar assinatura] --> B(Trial)
    B -- cobrar --> C{a cobrança passou?}
    C -->|sim| D[Ativa]
    C -->|não, e ainda há prazo| E[Incompleta]
    C -->|não, e o prazo acabou| F[Cancelada]
    E -- tentar de novo --> B
```

A assinatura nasce em **trial** e vence em um mês. Na cobrança, passando, ela vira **ativa** e o
vencimento anda mais um mês; falhando, ela fica **incompleta** enquanto houver prazo de tolerância, e
é **cancelada** quando ele acaba. Cada passagem de estado registra um evento de domínio.

## Como rodar

A infraestrutura é a do monorepo, com o `.env` da raiz. Deste serviço o compose sobe dois
containers: a aplicação e o banco dela.

```bash
docker compose up -d subscription-mysql keycloak    # da raiz do monorepo
./gradlew bootRun                                   # a API, na 8084
```

Para rodar a aplicação em container também, empacote o jar antes — a imagem não compila:

```bash
./gradlew bootJar
docker compose up -d subscription-api               # da raiz do monorepo
```

```bash
./gradlew build                 # compila e roda tudo; porta de entrada de cada commit
./gradlew unitTests             # só os que não sobem container (dispensa Docker)
./gradlew integrationTests      # só @IntegrationTest
./gradlew e2eTests              # só @E2ETest
./gradlew bootJar               # build/libs/application.jar, na raiz do serviço
```

## A coleção de requisições

`.postman/subscription-api-codeflix.postman_collection.json` é a regressão manual da API, e só vale
completa: rota nova entra nela no mesmo trabalho. Rode a pasta `0. Autenticação` primeiro — ela
guarda o token nas variáveis da coleção, e as outras o usam.

Preencha `clientSecret` com o `KEYCLOAK_CLIENT_SECRET` do `.env`; o arquivo versionado vai com o
campo vazio.

Como não há remoção de plano, a coleção não limpa o que cria: rodar de novo acrescenta planos, sem
quebrar nada e sem depender do que ficou da execução anterior.

## Por que banco próprio

O MySQL da `admin-api` já existe, já tem binlog ligado e já tem o usuário do Debezium: um schema a
mais nele teria saído de graça. Ainda assim o banco aqui é outro container, pelo mesmo motivo que
levou o codificador a ter o Postgres dele:

- **O estado de um serviço não é assunto do outro.** Com um banco só, uma migration daqui alcança as
  tabelas de lá, e um `flyway clean` desatento apaga o catálogo inteiro.
- **Derrubar um não derruba o outro.** O banco da assinatura pode reiniciar, encher ou travar sem
  levar a administração do catálogo com ele.
- **O conector do Debezium fica menor.** Cada banco tem o seu, com a sua lista de tabelas, e o
  tópico de um não mistura evento do outro.

O preço está declarado: um container, um volume e um conector a mais, e o `CDC_USER` do `.env` passa
a ser criado nos dois bancos.

## Persistência sem ORM

Os agregados são salvos por SQL escrito à mão, com o `JdbcClient` do Spring. Não há entidade
espelhando a tabela nem contexto de persistência: o repositório lê o `ResultSet` direto para dentro
do agregado, pelo `with(...)` dele.

Duas consequências que valem registrar, porque mordem:

- **O lock otimista é explícito.** Cada tabela tem uma coluna `version`, e o update termina em
  `WHERE id = :id AND version = :version`. Zero linhas afetadas significa que alguém escreveu antes,
  e não que o id não existe.
- **Escrita precisa de commit.** O pool entrega a conexão com `auto-commit` **ligado**, ao contrário
  do da `admin-api`: lá o Hibernate abre transação em toda escrita, e aqui não há quem o faça. Com o
  auto-commit desligado, um update solto seria desfeito ao devolver a conexão, sem erro no log.
  Escrita que toca mais de uma tabela — o agregado mais a fila de eventos — declara `@Transactional`,
  e aí o gerenciador de transação desliga o auto-commit pelo tempo dela.

## Migrações

Mudança de schema é migration Flyway em `infrastructure/src/main/resources/db/migration`, nomeada
`V<n>__<descricao_em_snake_case>.sql`. O Flyway roda na subida da aplicação, pelo starter do Boot —
não há task `flywayMigrate` separada como na referência do curso.

Nunca editar migration já commitada: o checksum muda e o Flyway para em estado de reparação. O
ajuste é uma migration nova.

## O cadastro

```mermaid
flowchart TD
    A[POST /accounts/sign-up] --> B{o e-mail já tem conta?}
    B -->|sim| C[202, e nenhum e-mail sai]
    B -->|não| D{já existe no Keycloak?}
    D -->|sim| E[reaproveita a pessoa de lá]
    D -->|não| F[cria no Keycloak com a senha]
    E --> G[guarda o pendente e publica a validação]
    F --> G
    G --> H[202: 'a validation email is on its way']
    H --> I[a pessoa clica no link] --> J[POST /accounts/sign-up/confirm]
    J --> K[a conta nasce, o e-mail vira conferido e a pessoa entra no grupo accounts]
```

| Rota | Quem alcança | O que faz |
|---|---|---|
| `POST /accounts/sign-up`, `POST /accounts/sign-up/confirm` | qualquer um, sem token | pede o cadastro. Responde **202** e a **mesma** mensagem sempre |
| `POST /accounts/sign-up/confirm` | qualquer um, sem token | confirma com o token do e-mail. É aqui que a conta nasce |

As duas são abertas porque não poderiam ser outra coisa: quem se cadastra ainda não tem usuário no
Keycloak para pedir token, e quem clica no link também não — é justamente a confirmação que marca o
e-mail como conferido.

### A resposta é sempre igual, de propósito

E-mail novo, e-mail que já existe no Keycloak e e-mail que já tem conta recebem **202 com a mesma
mensagem**. Qualquer diferença contaria a um estranho se um endereço está cadastrado. É por isso que
o caso de uso de cadastrar não devolve nada: o controller não tem como responder diferente por
acidente.

Quem pede o cadastro com o e-mail de outra pessoa não avança: a validação vai para a caixa de
entrada **dela**, e sem o token não há confirmação. E não se cria usuário quando o e-mail já existe
no Keycloak, então **ninguém sobrescreve a senha de outro** se cadastrando com o endereço dele.

### O token

Sorteado com `SecureRandom`, 32 bytes, em base64 de URL. O banco guarda só o **resumo SHA-256** — um
vazamento de leitura do banco não entrega confirmação de cadastro alheio. O que vai no e-mail é o
token em claro, e ele vive 24 horas.

Pedir o cadastro de novo é legítimo, porque a mensagem pode não ter chegado: o pedido novo apaga os
pendentes não confirmados daquele e-mail, então **só o link mais novo vale**.

Clicar no link duas vezes devolve a **mesma conta**, e não um erro.

### Por que a conta é gravada antes do pendente

A ordem é escolhida para que interrupção no meio se conserte por repetição, e não por intervenção.
Morrendo o processo entre as duas gravações, sobra conta sem confirmação — e repetir reconhece esse
estado e termina o serviço. Na ordem inversa sobraria pendente confirmado sem conta, com o token já
gasto, e a pessoa ficaria travada.

Por isso não há transação envolvendo as duas escritas: ela também não cobriria a chamada ao
Keycloak, que vem depois e não participa de transação de banco.

### A role de conta vem com a conta

A confirmação põe a pessoa no grupo `accounts` do Keycloak, que concede `CODEFLIX_ACCOUNT` — sem
isso ela teria conta aqui e **nenhuma rota alcançável**, nem para ler a própria conta, nem para
assinar.

O que ela **não** concede é acesso ao catálogo: isso é consequência de ter assinatura valendo, e vem
pelos eventos de domínio. Ver "Autenticação" para por que os dois são separados.

A concessão vem **depois** de a conta existir — na ordem inversa, uma interrupção deixaria alguém com
a role e sem conta aqui — e acontece também quando se clica no link de novo, que é o que faz a
repetição consertar uma interrupção entre gravar a conta e falar com o Keycloak.

### A conta, depois de existir

| Rota | Quem alcança | O que faz |
|---|---|---|
| `GET /accounts/me` | assinante ou administrador | a conta de **quem está chamando** |
| `PUT /accounts/me/billing-address` | assinante ou administrador | troca o endereço de cobrança |

**`/me`, e não `/{id}`.** Com id no caminho, um assinante poderia pedir a conta de outro, e toda
rota daqui teria de conferir propriedade — uma checagem fácil de esquecer na rota seguinte. A conta
sai do `sub` do token, que é o mesmo id do provedor de identidade que a conta guarda, e a pergunta
"esta conta é sua?" deixa de existir.

Token válido de quem não tem conta aqui — um service account, por exemplo — recebe **404**, não 401:
a autenticação está certa, conta é que não há.

O corpo da resposta não traz senha nem o id do provedor: a primeira nunca sai daqui, e o segundo é
detalhe de como a autenticação funciona. E `billingAddress` vem **nulo** quando a conta não tem
endereço, não como um objeto de campos vazios — o front precisa distinguir "não informou" de
"informou vazio".

### Quem envia o e-mail

Não é este serviço. Ele publica o pedido em `mail.events` com a routing key
`mail.signup.validation`, e a mensagem leva o endereço, o primeiro nome e o **link pronto**. Quem
entrega é um serviço de e-mail próprio, que ainda não existe — então, hoje, a mensagem fica na fila.

Para experimentar à mão: suba o RabbitMQ, faça o cadastro, e pegue o link em
`mail.signup.validation.queue` pelo painel do broker, na 15672.

O link aponta para a página do front (`SIGNUP_CONFIRMATION_URL`), não para uma rota desta API: é o
front que chama o `POST` de confirmar com o token. Front também não existe ainda.

## As rotas do plano

O plano é administração: quem o cria e altera é o operador do catálogo.

| Rota | O que faz |
|---|---|
| `POST /plans` | cria. Sem o campo `active`, o plano nasce **fora de venda** |
| `GET /plans/{id}` | busca pelo id, inclusive fora de venda |
| `PUT /plans/{id}` | troca nome, descrição e preço. **Não aceita `active`** |
| `PUT /plans/{id}/activate` | põe à venda. Idempotente |
| `PUT /plans/{id}/deactivate` | tira de venda. Não apaga nada |

Três decisões aí dentro que divergem da referência do curso:

- **Ativar e desativar têm rotas próprias**, e o `PUT` do recurso não decide ativação. No curso o
  corpo da alteração carrega um booleano `active` que escolhe entre ativar e desativar; os
  `CLAUDE.md` do monorepo proíbem booleano escolhendo entre dois comportamentos.
- **Criar sem `active` nasce fora de venda**, ao contrário das categorias da `admin-api`, que nascem
  ativas: um plano ativo é um plano à venda, e pôr algo à venda é ato deliberado.
- **O preço é `BigDecimal`**, não `Double`: a coluna é `DECIMAL(15,2)` e dinheiro em ponto flutuante
  acumula erro em soma e comparação.

Os status de erro são os de sempre: `422` para dado que o domínio recusa, `404` para plano que não
existe e `409` quando a gravação perdeu a corrida do lock otimista.

### Não há remoção de plano

De propósito, e é a razão de a coleção de requisições não limpar o que cria: plano referenciado por
assinatura não pode desaparecer, e tirar de venda já resolve o caso de uso. Quem precisar zerar o
banco de desenvolvimento recria o volume.

## As rotas da assinatura

Todas são sobre a assinatura de **quem está chamando**, pela mesma razão do `/me` da conta: nenhuma
recebe id de assinatura, então não existe caminho para pedir a de outro.

| Rota | O que faz |
|---|---|
| `POST /subscriptions` | assina um plano. Nasce em trial, vencendo em um mês |
| `GET /subscriptions/me` | a assinatura de quem chama, inclusive a cancelada |
| `POST /subscriptions/me/charge` | cobra, se estiver vencida |
| `PUT /subscriptions/me/cancel` | cancela. Idempotente |

**Assinar não cobra.** A assinatura nasce em trial com um mês pela frente; cobrar é ato separado, e
é o que a faz virar ativa.

**Cobrança recusada responde `200`**, não `4xx`. A requisição foi atendida e o serviço fez o que
devia — quem recusou foi o provedor. O resultado está no corpo:

```json
{ "subscription": { "status": "incomplete" }, "charged": false,
  "transactionId": "fake-…", "reason": "the card was refused" }
```

É esse `reason` que o front mostra para a pessoa trocar o cartão. Assinatura **não vencida** responde
`charged: false` com `transactionId` nulo, sem sequer chamar o provedor: cobrar antes do vencimento
seria cobrar duas vezes no mês.

Conta **sem endereço de cobrança** recebe `422` dizendo o que falta, em vez de uma tentativa que
falharia no provedor. Quem assinou precisa chamar `PUT /accounts/me/billing-address` antes de cobrar.

Quem já tem assinatura não cancelada recebe `409` ao assinar de novo. **Cancelada não tranca a
conta**: reassinar é legítimo, e a assinatura nova é outra.

A tolerância antes do cancelamento é `subscription.tolerance-days`, com **2** como padrão. `0`
cancela na primeira recusa.

### O provedor de pagamento é de mentira

Não há provedor de verdade ainda. O que existe é um `FakePaymentGateway` que decide pelo **prefixo
do token do cartão**, e não um mock que aprova sempre — é o que torna os três caminhos do ciclo
exercitáveis à mão:

| Token do cartão | O que acontece |
|---|---|
| `tok_refused…` | recusado pelo provedor, com transação aberta |
| `tok_unreachable…` | recusado sem transação: o provedor não respondeu |
| qualquer outro | aprovado |

Ele avisa no log a cada cobrança que é de mentira, e **não sobe em produção**: sem provedor de
verdade configurado, o perfil `production` não sobe. Não subir é melhor que subir aprovando toda
cobrança em silêncio.

O corpo da cobrança leva **só o token do cartão**. Número e CVV não têm campo onde entrar — quem os
troca por um token é o provedor, direto com o navegador de quem assina —, e isso mantém o serviço
fora do escopo do PCI DSS.

### Para ver o ciclo inteiro à mão

O vencimento é de um mês, então à mão só se vê a cobrança recusar por não estar vencida. Para ver a
virada do estado, a pasta `3. Assinatura` da coleção cobre as respostas, e a jornada automatizada
(`SubscriptionE2ETest`) adianta o relógio e passa por trial → ativa → incompleta → cancelada.

## Autenticação

O realm é o mesmo dos outros serviços, em `../provisioning/keycloak/realm.json`, e as roles que
importam aqui são `CODEFLIX_ADMIN` e `CODEFLIX_ACCOUNT`.

**`CODEFLIX_ACCOUNT` e `CODEFLIX_SUBSCRIBER` são coisas diferentes**, e confundi-las foi um defeito
de verdade. A primeira diz "tem conta neste serviço" e é o que abre as rotas daqui; a segunda diz
"pode assistir" e é o que a `videos-api` exige nas queries do catálogo.

Enquanto as rotas daqui exigiam `CODEFLIX_SUBSCRIBER`, concedê-la na confirmação do cadastro era
obrigatório — sem ela ninguém conseguiria nem assinar. E aí quem só confirmava um e-mail ganhava o
catálogo inteiro, sem assinar e sem pagar, e cancelar não tirava nada. Agora a confirmação concede a
de conta, e a do catálogo vem e sai com a assinatura, pelos eventos de domínio. Dois testes guardam
isso: a role do catálogo não abre rota nenhuma daqui, e confirmar cadastro não a concede.

A divisão das rotas é essa:

| Rota | Quem alcança |
|---|---|
| `POST /accounts/sign-up`, `POST /accounts/sign-up/confirm` | qualquer um, sem token |
| `/plans/**` | `CODEFLIX_ADMIN` |
| `/accounts/**`, `/subscriptions/**` | `CODEFLIX_ADMIN` ou `CODEFLIX_ACCOUNT` |
| a documentação da API | qualquer um (e em produção ela está desligada) |

Token válido de um client de **outro** serviço — o da `videos-api`, por exemplo — recebe `403`: ele
não tem nenhuma das duas roles daqui. Já um token que tem a role mas não tem conta neste serviço
recebe `404`, porque aí a autorização passou e conta é que não há.

As duas do cadastro são as únicas rotas abertas, e não poderiam ser outra coisa: quem se cadastra
ainda não tem usuário no Keycloak para pedir um token — é justamente o cadastro que cria o usuário
lá — e quem clica no link do e-mail também não.

A segurança fica ligada em **todos** os perfis, inclusive no `development`. A referência do curso a
desliga com `@Profile("!development")`, e isso deixaria o controle de acesso sem ser exercitado
justamente no ambiente em que se trabalha.

### Não há fluxo de senha direta no realm

Nenhum client do realm versionado aceita usuário e senha, e isso é escolha: o fluxo de senha direta
foi depreciado no OAuth 2.1, e um front de navegador deve usar código de autorização com PKCE.

A consequência prática é que **não se obtém token de uma pessoa pela linha de comando** — por isso a
coleção de requisições usa o token de client, cujo `sub` não é pessoa nenhuma, e as rotas `/me`
respondem `404` para ele. A jornada automatizada precisa de token de pessoa de verdade, e cria um
client só para isso **dentro do container de teste**; ele não existe em
`../provisioning/keycloak/realm.json`.

## Os eventos, e como eles saem daqui

Cada transição da assinatura registra um evento de domínio, e é por eles que o **acesso ao catálogo**
é concedido e revogado. Esse acesso não vem do cadastro: quem confirma um e-mail recebe a role de
**conta**, que só alcança as rotas daqui.

```mermaid
flowchart LR
    A[agregado salvo] -->|mesma transação| B[(tabela event)]
    B -->|binlog| C[Debezium]
    C --> D[Kafka]
    D --> E[EventCdcListener]
    E -->|Created, Renewed| F[concede o catálogo]
    E -->|Canceled| G[revoga o catálogo]
```

| Evento | O que acontece com o catálogo |
|---|---|
| `SubscriptionCreated` | concedido |
| `SubscriptionRenewed` | concedido de novo — quem tinha perdido volta a ter, sem caso especial |
| `SubscriptionIncomplete` | **nada**: a tolerância existe para a pessoa resolver o cartão |
| `SubscriptionCanceled` | revogado |
| `AccountCreated` | nada; ninguém o consome ainda |

**A fila de saída é o que faz "gravou e avisou" ser atômico.** Publicar num broker depois do commit
perde o evento se o processo morrer no meio; publicar antes avisa de algo que pode não ter
acontecido. Gravando na tabela `event` dentro da mesma transação do agregado, ou as duas escritas
acontecem ou nenhuma — e quem leva o evento adiante é o Debezium lendo o binlog, já depois de
confirmado. Há teste provando a transação, e ele fica vermelho se o `@Transactional` sair.

**Sem coluna de processado**, ao contrário da referência do curso. Lá o consumidor lê a linha de
volta pelo id e a marca, o que custa uma ida ao banco por mensagem e abre uma corrida entre dois
consumidores lendo a mesma linha. Aqui a mensagem do CDC já traz a linha inteira e o consumidor age
direto; o que torna a repetição inofensiva é o handler ser **idempotente** — conceder ou revogar
grupo no Keycloak pode acontecer duas vezes sem efeito. Quem guarda o progresso é o offset do Kafka.

**Uma partição só**, como o `provisioning/cdc` já configura: é ela que garante a ordem dos eventos de
uma mesma assinatura. Com duas, `canceled` poderia ser processado antes de `renewed` e o acesso
ficaria errado.

### Pôr o CDC de pé

O conector não mora em arquivo da aplicação: é registrado na API do Kafka Connect, com a
configuração versionada em `../provisioning/cdc/subscription-mysql-cdc.json`, cujos marcadores de
usuário e senha são trocados na hora pelos valores do `.env`:

```bash
docker compose --profile cdc up -d kafka kafka-connect   # da raiz do monorepo

CU=$(grep '^CDC_USER=' .env | cut -d= -f2-); CP=$(grep '^CDC_PASSWORD=' .env | cut -d= -f2-)
python3 -c "
import json
cfg = json.load(open('provisioning/cdc/subscription-mysql-cdc.json'))
cfg['database.user'] = '$CU'; cfg['database.password'] = '$CP'
print(json.dumps(cfg))"   | curl -sS -X PUT http://localhost:8083/connectors/subscription-mysql-cdc/config       -H 'Content-Type: application/json' --data-binary @-

curl -sS http://localhost:8083/connectors/subscription-mysql-cdc/status   # deve dizer RUNNING
```

O `database.server.id` é **10001**, diferente do 10000 do conector do admin: dois conectores com o
mesmo id brigam pela mesma posição no binlog do servidor e um derruba o outro.

### Nota sobre o CDC

O usuário que o Debezium usa para ler o binlog nasce de `init/10-cdc-user.sh`, e o entrypoint da
imagem do MySQL **só executa esse script na criação do volume**. Em banco que já existe ele não
roda: nesse caso, rode o SQL à mão, ou recrie o volume com `docker compose down -v`, que apaga os
dados.
