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

## Quando o disco enche: o que acontece e como voltar

Aconteceu, e a sequência de recuperação não é óbvia — o README já avisava da primeira armadilha, e as
outras três só aparecem no meio do incidente.

O disco passou de **88%**, o kubelet declarou `DiskPressure` e despejou **46 pods** entre `codeflix`
e `istio-system`. O sintoma na porta é `503 upstream connect error` em todas as rotas, com os pods
que sobraram aparentemente saudáveis.

A ordem importa, e cada passo tem um porquê:

| passo | por quê |
|---|---|
| `docker builder prune -f` | o cache de build é o maior pedaço descartável de quem anda construindo imagem. Aqui foram 6,7 GB, e levou o disco de 88% para 83% |
| `kubectl delete pods --field-selector status.phase=Failed` nos **dois** namespaces | pod despejado não volta sozinho, e o `istio-system` também é despejado — 24 dos 46 eram dele |
| `bash deploy/cluster/import-images.sh` | **o kubelet coleta as imagens carregadas à mão, e o rótulo `pinned` não resiste à pressão.** Ele não as removeu de um nó só: faltavam 2 num, 6 noutro e 6 no terceiro |
| `kubectl rollout restart` em cada aplicação | **esta é a que custa achar.** O `istiod` reinicia no despejo, e os sidecars precisam de certificado novo; sem o reinício o gateway segue dando 503 com o pod servindo normalmente por dentro. Reiniciar **só** o `istio-ingressgateway` não resolve |

Dois detalhes que economizam tempo:

- **o pod velho continua servindo**, e isso engana: `kubectl exec ... wget localhost:8082` responde certo,
  o Service tem endpoint, a sonda diz `Ready` — e o gateway dá 503. A diferença está no mTLS entre os
  dois Envoy, não na aplicação;
- **reiniciar antes de reimportar trava o rollout**: o pod novo nasce em `ImagePullBackOff` porque a
  imagem não está mais no nó, e o antigo fica. `kubectl rollout undo` desfaz enquanto a imagem não
  voltar.

O caminho que evita tudo isso é o que o README já aponta como o certo: um **registro local do k3d**,
em vez de carregar imagem por tarball. Com registro, a coleta do kubelet é inofensiva — a imagem tem
de onde voltar.

## Cada carga tem identidade própria, e isso só aparece ao escrever a primeira política

Os 18 workloads rodavam com o service account `default`. Isso não incomoda em nada — até a primeira
tentativa de `AuthorizationPolicy`: o certificado que o istiod emite carrega a identidade do service
account, então com `default` em todos eles o certificado de **todo** pod dizia a mesma coisa.

```
antes:  spiffe://cluster.local/ns/codeflix/sa/default      (os 18)
depois: spiffe://cluster.local/ns/codeflix/sa/videos-api   (lido do certificado do pod)
```

Com uma identidade só, "apenas o catálogo pode chamar o admin" é **inexprimível**: dava para dizer
"alguém da malha", e nada além. O mTLS `STRICT` continua valendo o que vale — ele cifra e exige
certificado —, mas o que ele não dava sozinho era identidade por carga.

Um service account por workload, com o mesmo nome do Deployment, para não haver mapa a consultar.
Eles entram **antes** de qualquer Deployment no `up.sh`: pod que referencia service account
inexistente não é criado, e o erro — "error looking up service account" — fica no evento do pod, não
no rollout.

Nenhum deles recebe permissão de API do Kubernetes. Service account aqui é **só** identidade de rede.

Conferido depois da troca: as rotas pelo gateway, os fronts por NodePort, os 18 pods prontos, os 18
alvos de métrica — todos como antes.

## Quem pode chamar quem

O par do mTLS: ele diz que o chamador tem identidade verificável, e a `AuthorizationPolicy` decide o
que ele pode fazer com ela. Sem a segunda, a primeira só garante que o chamador é **alguém** da
malha — e hoje o portal poderia abrir conexão no MySQL do admin.

**O grafo não foi adivinhado.** Saiu das métricas do Istio, do que de fato aconteceu neste cluster:

```bash
kubectl -n istio-system exec deploy/prometheus -c prometheus-server -- wget -qO- \
  'http://localhost:9090/api/v1/query?query=sum(istio_requests_total)by(source_workload,destination_workload)'
```

16 pares HTTP, e — na métrica **separada** `istio_tcp_connections_opened_total` — 14 pares TCP. Os
TCP são a parte que mais importa e quase me escaparam: eles **não** aparecem em
`istio_requests_total`. Uma política escrita só a partir do HTTP bloquearia todo banco, toda fila e o
índice, e o sintoma seria a aplicação subindo e não falando com nada.

`nada-passa-sem-regra` é um `ALLOW` sem regra nenhuma, que é o idioma do Istio para negar tudo: a
partir dele, workload sem política própria não recebe chamada. O `kafka-connect` é um desses, e de
propósito — nada o chama pela malha.

### O que foi medido

Da **mesma** origem, um permitido e dois negados, que é o que mostra que a política discrimina por
identidade e não por sorte:

```
kafka-connect -> mysql:3306        conectou   (está na lista do mysql)
kafka-connect -> videos-api:8082   403        (não está na lista do catálogo)
kafka-connect -> garage:3900       403        (nem na do armazenamento)
```

E os fluxos inteiros, com a política valendo: as rotas pelo gateway, os fronts por NodePort, o
cadastro (`202`), a mídia atravessando `gateway -> catálogo -> admin -> armazenamento`, e o **CDC de
ponta a ponta** — uma categoria criada no admin chegou ao catálogo em cerca de 5 segundos. 18 pods
prontos, 18 alvos de métrica, nenhum 403 fora dos testes.

### Três coisas que eu errei antes de acertar

**O Kafka precisa estar na própria lista.** O `advertised.listeners` dele é `INTERNAL://kafka:9092`,
então todo cliente — inclusive o `kafka-topics.sh` rodando **dentro** do pod — pede metadado e
reconecta pelo nome do serviço, saindo pelo sidecar com identidade `sa/kafka`. Sem a regra:

```
kafka-topics.sh --bootstrap-server localhost:9092 --list
-> Timed out waiting for a node assignment. Call: listTopics
```

O quórum do controlador **não** precisa de regra — é `1@localhost:9093`, e localhost não passa pelo
sidecar. Foi a primeira coisa que suspeitei, e estava errado.

**E esse erro meu produziu um diagnóstico errado.** Com o `kafka-topics.sh` bloqueado, minha
verificação de tópicos devolveu zero, e eu conclui que o Kafka tinha perdido tudo. Tinha 44 tópicos.
O que contei foram as linhas de um comando que falhou, com o erro mandado para `/dev/null`.

**O `port-forward` não precisa de exceção.** Eu previ que o `register-cdc.sh` quebraria, porque ele
alcança o Connect por `kubectl port-forward` e isso não tem identidade. Funcionou: o port-forward
entra pelo namespace de rede do pod, como localhost, e não passa pelo sidecar inbound.

### Quando o Kafka Connect fica preso

Sintoma: conector `UNASSIGNED`, worker em laço com "unknown topic or partition" em
`connect_statuses-N`. Causa: o Kafka reiniciou, perdeu os tópicos internos do Connect, e eles foram
recriados com 1 partição enquanto o worker espera 5.

```bash
kubectl -n codeflix scale deploy/kafka-connect --replicas=0
for t in connect_configs connect_offsets connect_statuses; do
  kubectl -n codeflix exec deploy/kafka -c kafka -- \
    /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic "$t"
done
kubectl -n codeflix scale deploy/kafka-connect --replicas=1
bash deploy/infra/register-cdc.sh
```

### O risco desta abordagem, dito antes de alguém descobrir

Métrica mostra o que **rodou**, não o que **existe**. Caminho que ninguém exercitou na janela medida
não entra na lista e passa a ser recusado — com `403` no HTTP e conexão fechada no TCP.

Por isso o grafo medido foi **conferido contra o que cada serviço declara depender**, lendo as
variáveis de ambiente que os manifestos dão a ele:

| serviço | destinos declarados | na política |
|---|---|---|
| `admin-api` | mysql, rabbitmq, garage, keycloak | sim |
| `videos-api` | admin-api, catalog-elasticsearch, kafka, keycloak | sim |
| `subscription-api` | subscription-mysql, kafka, rabbitmq, keycloak | sim |
| `encoder-api` | encoder-postgres, rabbitmq, garage | sim |
| `mail-api` | rabbitmq, mailpit (SMTP) | sim |

As duas listas fecham, e é isso que dá confiança de que a janela medida não escondeu caminho.

**Um detalhe que a conferência revelou:** os três serviços Java alcançam o Keycloak pelo **nome
público** — `keycloak.localtest.me` —, e não pelo nome de serviço do cluster. Então aquele tráfego
pode entrar no Keycloak vindo do gateway, e não direto do serviço. A lista dele permite **os dois**,
e por isso o caminho fecha de qualquer forma; se permitisse só um, funcionaria ou não dependendo de
como o DNS do cluster estivesse resolvendo naquele dia — que é o tipo de dependência invisível que
não se quer numa regra de segurança.

Caminho novo entre serviços precisa de regra nova, e o sintoma de esquecer é um `403` que o Jaeger
agora mostra em qual salto aconteceu.

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

## O rastreamento: o que ligou, e onde ele ainda quebra

O Jaeger subia com o perfil `demo` e o meshconfig já declarava o provedor apontando para
`jaeger-collector:4317`. **Faltava o que liga uma coisa à outra:** sem um recurso `Telemetry` nenhum
sidecar manda span, e `defaultProviders` do meshconfig lista só métricas. Um Jaeger de pé,
alcançável pelo gateway, e vazio — que é pior que não ter, porque parece instalado.

`deploy/mesh/telemetry.yaml` resolve isso. Medido num Jaeger recém-reiniciado, o mesmo tráfego nos
dois estados:

| | serviços conhecidos pelo Jaeger |
|---|---|
| sem o `Telemetry`, 40 requisições pela malha | **0** |
| com o `Telemetry` | **5**, incluindo `catalog-elasticsearch` e `keycloak` |

**Para repetir esta medição, reinicie o pod do Jaeger primeiro.** O addon guarda os traces em
memória, então apagar o `Telemetry` não apaga o que já entrou — e a medição do "antes" sai
contaminada com o "depois" de minutos antes. Foi o que aconteceu na primeira tentativa aqui, e o que
a desmascarou foi conferir a idade do pod:

```bash
kubectl -n istio-system rollout restart deploy/jaeger
kubectl -n istio-system get pods -l app=jaeger   # o nome TEM de mudar
```

**E aí aparece o limite, que é bom conhecer antes de confiar na ferramenta.** Nos 30 traces que uma
consulta GraphQL do catálogo gerou, há exatamente **duas** formas, e nenhuma delas liga as duas
pontas:

| forma | ocorrências | spans | serviços |
|---|---:|---|---|
| `29486cbf...` | 14x | 3 | gateway -> gateway (egress) -> **videos-api** |
| `34e5a6ad...` | 16x | 2 | **videos-api** -> catalog-elasticsearch |

**Nenhum trace contém o gateway e o Elasticsearch juntos.** A chamada ao índice que atendeu uma
requisição está sempre num rastro à parte — não às vezes, sempre.

A causa é conhecida e não é do Istio: o sidecar de saída vê um pedido **sem** cabeçalho de rastro —
porque a aplicação não repassou o que recebeu — e começa um rastro novo. Nenhum dos cinco serviços
propaga: os três Java não têm `micrometer-tracing`, e nos dois Go o OpenTelemetry aparece só como
dependência transitiva.

### E o conserto: o rastro passou a atravessar

O repasse entrou nos serviços que podem fazê-lo, e o resultado é a mesma consulta GraphQL, no mesmo
cluster, antes e depois:

| | traces | formas | ligando as duas pontas |
|---|---:|---|---:|
| antes | 30 | 2 (gateway+catálogo, catálogo+índice) | **0** |
| depois | 9 | 1 (gateway+catálogo+índice, 7 spans) | **8 de 9** |

Os 30 fragmentos viraram 8 árvores completas — o número de traces **caiu** porque cada um agora
cobre a requisição inteira, em vez de um salto. Uma delas, por dentro:

```
istio-ingressgateway   videos-api...:8082/*
istio-ingressgateway   router outbound|8082||videos-api...
videos-api             videos-api...:8082/*
videos-api             catalog-elasticsearch...:9200/*
catalog-elasticsearch  catalog-elasticsearch...:9200/*
videos-api             catalog-elasticsearch...:9200/*
catalog-elasticsearch  catalog-elasticsearch...:9200/*
```

E os outros dois caminhos, medidos do mesmo jeito:

| caminho | spans |
|---|---|
| `gateway -> catálogo -> admin -> armazenamento` (mídia) | **7**, todos em 200 |
| `gateway -> assinatura -> Keycloak` (cadastro) | **9**, com as três chamadas ao Keycloak na mesma árvore |

Quatro serviços numa árvore só, no primeiro — que é o caminho onde "quem demorou" nunca tinha
resposta.

O 9º trace é a busca de chaves no Keycloak, que acontece **fora** de qualquer requisição — não há
rastro para continuar ali, e ele ficar solto é o comportamento certo.

### Três coisas que só a medição no cluster mostrou

**A thread importa, e o teste de unidade não a revela.** O repasse lê a requisição de entrada do
`RequestContextHolder`, que é um ThreadLocal. Com ele ligado e os testes verdes, medido no cluster:

- a rota de mídia **juntava** o rastro — o controller chama o admin na thread da requisição;
- a consulta GraphQL **continuava partida** — o graphql-java executa os data fetchers em outra
  thread, onde o ThreadLocal está vazio.

O que fechou isso foi registrar o `RequestAttributesThreadLocalAccessor` no `ContextRegistry`: o
Spring GraphQL carrega ThreadLocal para a thread de execução, mas só os que estiverem registrados. O
acessador já vem no spring-web; o registro não vem, porque não há arquivo de serviço.

**No cliente do Elasticsearch, o gancho óbvio é o errado.** Ele é assíncrono, e o interceptor de
*requisição* do Apache HttpClient roda na thread de I/O do reator — longe do ThreadLocal. Medido:

```
exec-interceptor     -> thread do chamador      contexto: visível
request-interceptor  -> httpclient-dispatch-1   contexto: null
```

O gancho de `exec` roda antes de a requisição ir para o reator, e é o que funciona.

**No armazenamento, o rastro não pode ser assinado — e eu assinei.** O SDK da AWS assina a requisição
com SigV4, e o gancho óbvio (`ExecutionInterceptor.modifyHttpRequest`) roda **antes** disso, então o
cabeçalho entrava em `SignedHeaders`. O Garage passou a recusar **todo** pedido de mídia com `403
Invalid signature`, porque o sidecar **reescreve** `traceparent` em trânsito — ele cria um span novo
para o salto e troca o valor. Assinatura sobre cabeçalho que a malha reescreve nunca corresponde.

O rastro passou para a camada do cliente HTTP, que o SDK chama depois de assinar. Isto quebrou o
cluster por alguns minutos e o teste não pegou: ele afirmava que a requisição continuava assinada, o
que era verdade e não provava nada, porque dublê não confere assinatura.

### Onde o repasse não existe, e por que não é escolha

| serviço | entrada | saída | repassa? |
|---|---|---|---|
| `videos-api` | HTTP | HTTP (admin, índice) | **sim** |
| `admin-api` | HTTP | HTTP (armazenamento) | **sim** |
| `subscription-api` | HTTP | HTTP (Keycloak) | **sim** |
| `encoder-api` | AMQP | HTTP (armazenamento) | não há o que repassar |
| `mail-api` | AMQP | SMTP | não há o que repassar |

Os dois Go ficam de fora por **mecanismo**, não por decisão: o encoder é acionado por mensagem de
fila, não por requisição HTTP, então não existe rastro de entrada para continuar; e o mail sai por
SMTP, que o Istio nem rastreia. Ligá-los exigiria propagar o rastro **pela mensagem** — outro
problema, com outra solução.

**Amostragem a 100%, e o preço.** Num cluster de desenvolvimento o valor é abrir a requisição que
acabou de falhar; com o padrão de 1% ela praticamente nunca está lá. O addon do Jaeger guarda os
traces **em memória**, então um teste de carga a centenas de requisições por segundo o enche e começa
a descartar. Antes de rodar `perf/`, baixe a amostragem:

```bash
kubectl -n istio-system patch telemetry rastreamento --type=merge \
  -p '{"spec":{"tracing":[{"providers":[{"name":"jaeger"}],"randomSamplingPercentage":1}]}}'
```

## O que falta

- **mTLS está `PERMISSIVE`**: a malha cifra quando os dois lados falam Istio, mas aceita texto puro.
  `STRICT` é um `PeerAuthentication` pequeno e é o maior ganho isolado.
- **O Kafka não tem volume.** MySQL, Elasticsearch e Garage têm PVC; ele não, então todo reinício do
  pod leva os tópicos. Isso não perde dado de negócio — o que está no MySQL e no índice sobrevive, e
  o CDC reconstrói —, mas **deixa o Kafka Connect preso**: os tópicos internos dele são recriados com
  1 partição pelo padrão do broker enquanto o worker espera 5, e ele fica em laço buscando
  `connect_statuses-1..4`. A recuperação está na seção "Quem pode chamar quem"; o conserto é dar
  volume a ele.
- **O rastreamento das filas.** O das requisições HTTP está fechado: os spans chegam e as
  requisições atravessam os serviços num trace só, medido acima. O que continua solto é o que passa
  por Kafka e RabbitMQ — o encoder e os listeners de CDC. Ligá-los é propagar o rastro **pela
  mensagem**, que é outro problema: o Istio não rastreia AMQP nem o protocolo do Kafka, então o
  contexto teria de ir num cabeçalho da própria mensagem e ser lido pelo consumidor.
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
