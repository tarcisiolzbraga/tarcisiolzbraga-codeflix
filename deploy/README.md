# Implantação em Kubernetes, com Istio

O Codeflix inteiro num cluster local: as dez peças de apoio, os sete aplicativos, e a malha medindo
tudo. Um comando:

```bash
bash deploy/build-images.sh   # as sete imagens, com as URLs do cluster embutidas nos fronts
bash deploy/up.sh             # sobe do zero, semeia e imprime os endereços
bash deploy/down.sh           # derruba o cluster (leva os dados com ele)
```

O `build-images.sh` **não** é o mesmo que `docker compose build`, e a diferença só aparece quando
alguém abre o front: as URLs das APIs são embutidas no JavaScript em tempo de build. Um build comum
produz fronts apontando para `localhost:8080` — certo para o compose, errado para o cluster.

Isto **não substitui** o `docker compose` da raiz, que continua sendo o caminho de quem está
programando: ciclo mais curto, nada para construir em imagem. O cluster existe para ver o sistema
como um sistema — quem chama quem, com que latência, e o que acontece quando uma peça cai.

## O que fica de pé

| | |
|---|---|
| Cluster | k3d (k3s em Docker), 3 nós, Traefik desligado |
| Malha | Istio, perfil `demo`, com Prometheus, Kiali, Jaeger e Grafana |
| Namespace `codeflix` | 17 pods, todos com sidecar |
| Apoio | mysql ×2, postgres, keycloak, rabbitmq, kafka + connect, elasticsearch, garage, mailpit |
| Aplicativos | admin-api, videos-api, subscription-api, encoder-api, mail-api, portal, back-office |

```
portal          http://localhost:3000            back-office  http://localhost:3001
admin-api       http://admin-api.localtest.me    catálogo     http://catalogo.localtest.me/api/graphql
assinatura      http://assinatura.localtest.me   Keycloak     http://keycloak.localtest.me
Mailpit         http://mailpit.localtest.me      RabbitMQ     http://rabbitmq.localtest.me
Kiali           http://kiali.localtest.me/kiali/ Grafana      http://grafana.localtest.me
Jaeger          http://jaeger.localtest.me/jaeger/search
```

## Por que os fronts estão fora do gateway

Essa é a assimetria mais visível aqui, e ela não é descuido.

O `oidc-client-ts` calcula o desafio PKCE S256 com `crypto.subtle`, e **`crypto.subtle` só existe em
contexto seguro**: HTTPS, ou um endereço que o navegador trate como loopback. Servido como
`http://admin.localtest.me`, o painel morria antes de redirecionar:

```
Falha ao autenticar: Crypto.subtle is available only in secure contexts (HTTPS)
```

Então os dois fronts são publicados por NodePort em `localhost:3000` e `localhost:3001`. As APIs e o
Keycloak seguem atrás do gateway: nenhum deles precisa de contexto seguro.

**Tentei `*.localhost` primeiro**, que resolveria os dois lados de uma vez — o navegador o trata como
contexto seguro. Falhou por ser *reservado para loopback*: o resolvedor dentro do container responde
`127.0.0.1` para `keycloak.localhost` **sem consultar o DNS**, e o pod tenta falar consigo mesmo
(`connect to 127.0.0.1 port 80 failed`). A propriedade que resolve o navegador quebra o lado de
dentro.

O caminho definitivo é TLS no gateway, e aí tudo volta para um domínio só. O preço é o certificado
autoassinado ter de entrar no truststore das três JVMs, senão elas recusam o JWKS.

## O `iss` do token, que é o nó de tudo

O `iss` de um token é **a URL que o cliente usou** para pedi-lo, e quem valida busca as chaves
públicas na mesma URL. Browser e pod estão em redes diferentes, e `localhost` significa coisas
diferentes nas duas — foi exatamente isso que impediu as aplicações de rodarem em container no
compose.

A saída é um nome que vale nos dois lados:

```
de fora   keycloak.localtest.me -> 127.0.0.1 (DNS público) -> porta 80 -> gateway
de dentro keycloak.localtest.me -> CoreDNS do cluster      -> ClusterIP do gateway
```

O mesmo nome, a mesma porta. Conferido nas duas pontas: o `issuer` do
`.well-known/openid-configuration` é `http://keycloak.localtest.me/realms/codeflix` de fora **e** de
dentro de um pod. **Nenhuma linha dos três serviços Java mudou** — eles derivam o JWKS do
`issuer-uri` de propósito, e é por isso que o conserto é um bloco de CoreDNS e não um segundo
endereço na configuração deles.

## As armadilhas que cada arquivo registra

Cada uma destas custou tempo e está comentada onde importa:

| Onde | O que |
|---|---|
| `infra/databases.yaml` | **toda porta não-HTTP se chama `tcp-*`**. MySQL, Postgres e AMQP falam primeiro do lado do servidor; sem o nome certo o sidecar fareja protocolo e a conexão morre no tempo limite |
| `infra/messaging.yaml` | o controlador KRaft do Kafka fala com **ele mesmo por `localhost`**: pelo nome do Service, o Envoy embaralha a volta e o broker morre com "fatal error while waiting for the controller" |
| `infra/messaging.yaml` | a JVM do Kafka Connect precisa de **heap declarado**; o padrão ignora o limite do container e o pod é morto por OOM sem erro de aplicação |
| `infra/storage.yaml` | a imagem do Garage **não tem ENTRYPOINT**, e o compose o declara ao lado do `command`. Traduzir só metade dá exit 128 sem uma linha de log |
| `apps/java.yaml` | perfil **`homolog`**, não `development`: só ele lê `KEYCLOAK_ISSUER_URI` inteiro. O `development` monta `http://host:porta/...`, e `:80` no `iss` não bate com o que o Keycloak assina |
| `apps/java.yaml` | a `startupProbe` consulta o grupo **`readiness`**, não um grupo `startup`: o Spring só cria `liveness` e `readiness`. Medido — `/actuator/health/startup` responde 404, e uma sonda apontada para lá põe o pod em CrashLoopBackOff com a aplicação de pé |
| `apps/java.yaml` | o Istio **reescreve** as sondas: o manifesto diz `9090`, e o `kubectl get pod` mostra `/app-health/admin-api/startupz` na **15020**, do sidecar. Não é configuração perdida — é o `rewriteAppHTTPProbers`, que faz a sonda atravessar o proxy para funcionar com mTLS. Quem for depurar conferindo o pod vai achar que o manifesto não foi aplicado |
| `apps/java.yaml` | o catálogo pede token para chamar o admin: sem `KEYCLOAK_TOKEN_URI`, **todos** os eventos de CDC vão para a fila morta — com o conector e o Kafka saudáveis, e o catálogo respondendo vazio |
| `infra/keycloak.yaml` | o Keycloak espera o Postgres num **initContainer**. Sem ele subia antes do banco, morria com `SQLState: 08001` e era reiniciado — um restart em toda subida a frio, justo quando os sete aplicativos o procuram. O compose resolve isso com `depends_on: service_healthy`; o Kubernetes não tem equivalente |
| `infra/keycloak.yaml` | o Postgres 18 monta **`/var/lib/postgresql`**, e não `/var/lib/postgresql/data` como o `encoder-postgres` (que é 17). O 18 põe o PGDATA numa subpasta por versão; com o caminho antigo a imagem recusa subir dizendo que o mount está "unused" |
| `cluster/dns-nodes.sh` | o resolvedor **dos nós**. Sem ele nem a imagem de sandbox chega, e todo pod fica em `Init:0/2` com um evento que fala de imagem, não de DNS |
| `cluster/import-images.sh` | o kubelet **coleta imagem "não usada"** acima de 85% de disco, e imagem carregada à mão não tem de onde voltar. Por isso elas são fixadas (`io.cri-containerd.pinned=true`) e conferidas uma a uma |
| `infra/register-cdc.sh` | o Kafka nasce vazio: sem registrar o conector, o catálogo fica vazio **para sempre**, com tudo saudável |

## As sondas, e as três gambiarras que elas substituíram

Até a `admin-api` ganhar actuator, cada serviço media prontidão de um jeito diferente — e cada jeito
era errado de um jeito próprio:

| Serviço | O que a sonda usava | Por que não servia |
|---|---|---|
| `admin-api` | `GET /v3/api-docs` | o springdoc está **desligado no perfil `production`**, então a sonda que funciona em homologação some em produção |
| `videos-api` | `tcpSocket` na 8082 | prova que o Tomcat abriu a porta, não que a aplicação atende |
| `subscription-api` | `GET /plans/on-sale` | rota de **negócio** como sonda: consulta banco a cada 10s e amarra a saúde do pod ao catálogo de planos |

Hoje as três consultam o actuator, na porta de gerenciamento (9090, 9091 e 9092). Essa porta entra
no pod para o kubelet e o Prometheus a alcançarem, e **não** entra no Service nem no gateway: a
proteção dela é a topologia.

### Três sondas, com papéis diferentes

- **`startupProbe`** tolera a subida lenta — 30 tentativas de 10s. Enquanto ela não passa, as outras
  duas nem correm.
- **`readinessProbe`** tira de rotação sem matar, apertada (3 falhas), porque a subida já foi coberta
  pela de cima. É ela que o Spring põe em `OUT_OF_SERVICE` quando a parada começa.
- **`livenessProbe`** mata e reinicia, e consulta o grupo `liveness`, que de propósito **não** inclui
  banco nem broker: reiniciar não traz nenhum dos dois de volta.

Isso substitui o `initialDelaySeconds: 30` com `failureThreshold: 30` que a prontidão carregava.
Aquele arranjo dava 30 tentativas **para sempre**, e não só na subida: uma aplicação travada levava
5 minutos para sair de rotação.

### As métricas da aplicação chegam ao Grafana

Os pods declaram `prometheus.io/scrape`, `prometheus.io/port` e `prometheus.io/path`, e o sidecar
**junta** as métricas da aplicação com as dele num endereço só. Sem isso, o painel teria latência e
código de resposta vindos do Envoy e nada de dentro da JVM — nem heap, nem pool do Hikari.

### A parada

`terminationGracePeriodSeconds: 60`, igual à janela do compose e pelo mesmo motivo: a aplicação
espera até 30s pela requisição em voo, e o padrão do Kubernetes é 30 — sem margem, quem decide é o
relógio do kubelet, não o `timeout-per-shutdown-phase`, que ao menos registra o que abandonou.

### Conferido no cluster, e não só no YAML

As três aplicações subiram com as sondas novas e rolaram sem reinício:

```
admin-api-855f69b549-lfcqb          2/2  Running  0 restarts
subscription-api-5c79b54ffb-pgd98   2/2  Running  0 restarts
videos-api-55d9bfff86-q7drr         2/2  Running  0 restarts
```

**E a sonda foi provada por falha deliberada**, porque pod pronto não prova que a sonda mede algo —
o Istio a reescreve, e um proxy que respondesse por conta própria daria o mesmo verde. Apontando a
prontidão da `videos-api` para `/actuator/health/naoexiste`, o pod novo ficou **1/2 e nunca ficou
pronto**, com o antigo seguindo em rotação. Então a cadeia kubelet → sidecar → actuator está inteira,
e o rollout também está seguro: sonda errada não derruba quem está servindo.

As métricas da aplicação chegam ao Prometheus da malha, consultado de dentro do cluster:

| `application` | séries de `jvm_memory_used_bytes` |
|---|---|
| `admin-codeflix` | 8 |
| `videos-api-codeflix` | 8 |
| `subscription-codeflix` | 8 |

Um detalhe que economiza tempo de quem for olhar o painel: **`http_server_requests` nasce
preguiçosamente**, na primeira requisição à porta da aplicação, e as sondas **não** entram nele — o
servidor de gerenciamento não é instrumentado por essa métrica. Num pod recém-subido ela
simplesmente não existe, e isso não é defeito de configuração.

### Os dois serviços em Go seguem sem sonda

Não é esquecimento: eles não expõem HTTP, consomem fila. Não há porta onde pôr sonda, e o que o
cluster sabe deles é o processo estar de pé. Medi-los pede outro instrumento — profundidade de fila,
idade da mensagem mais velha —, e isso não está feito.

## O que ainda depende de rede

Nesta máquina os nós **não têm saída TCP para a internet** (ICMP passa, 443 não). As imagens de
plataforma — Istio, Kiali, Prometheus, Grafana, Jaeger, metrics-server — chegaram no primeiro
`istioctl install`, quando a saída funcionava, e hoje vivem no Docker do host:

```bash
bash deploy/cluster/export-platform-images.sh   # tira as imagens de um cluster vivo para o host
```

Com elas no host, `import-images.sh` as devolve a qualquer cluster novo, e a recriação não depende de
ninguém. Se faltar alguma, ele falha dizendo **qual** e como trazê-la.

## O ferramental

Nada é instalado por `sudo`; tudo em `~/.local/bin`:

| | |
|---|---|
| kubectl | 1.37.1 |
| k3d | 5.9.0 (k3s 1.35.5) |
| helm | 4.3.1 |
| istioctl | 1.31.3 |

## O que falta

- **mTLS está `PERMISSIVE`**: a malha cifra quando os dois lados falam Istio, mas aceita texto puro.
  `STRICT` é um `PeerAuthentication` pequeno e é o maior ganho isolado.
- **Nenhuma `AuthorizationPolicy`**: qualquer pod pode chamar qualquer pod — hoje o portal pode abrir
  conexão no MySQL do admin.
- **O rastreamento está instalado e vazio.** Falta o recurso `Telemetry` ligando o provedor, e os
  serviços não propagam `traceparent`/`b3` — sem isso cada salto vira um rastro solto em vez de uma
  árvore. É também o instrumento que pode explicar a intermitência do fluxo de mídia que o README da
  admin-api registra sem causa.
- **Nenhum HPA**, mas o caminho encurtou. O terreno estava pronto (metrics-server responde, todos os
  Deployments têm `requests.cpu`) e as três aplicações Java ganharam prontidão de verdade, que é o
  que faz réplica nova entrar em rotação na hora certa. O impedimento da `admin-api` **saiu**: o
  relay da outbox trava a linha com `SKIP LOCKED` e roda em mais de uma instância.
  Sobram dois: os tópicos de CDC têm uma partição só (réplica extra do catálogo fica ociosa no
  consumo), e o encoder pede escala **por profundidade de fila**, não por CPU. Ou seja, a `admin-api`
  e a `subscription-api` já podem escalar; o catálogo e o encoder não.
- **O limitador de requisição da `subscription-api` conta por instância.** Com réplica, o teto
  efetivo multiplica. Contador compartilhado pediria Redis; está dito no README da raiz.
- **Carregamento de imagem por tarball** é o ponto frágil: três cópias de 429 MB por recriação. O
  certo é um registro local do k3d, que exige recriar o cluster — por isso está documentado e não
  feito.
- **Sem cluster autoscaler**: três nós fixos, e a máquina com 31 GB. Aqui a escala serve para provar
  comportamento, não para ganhar capacidade.
