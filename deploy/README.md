# Implantação em Kubernetes, com Istio

O Codeflix inteiro num cluster local: as dez peças de apoio, os sete aplicativos, e a malha medindo
tudo. Um comando:

```bash
bash deploy/up.sh      # sobe do zero, semeia e imprime os endereços
bash deploy/down.sh    # derruba o cluster (leva os dados com ele)
```

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
| `apps/java.yaml` | o catálogo pede token para chamar o admin: sem `KEYCLOAK_TOKEN_URI`, **todos** os eventos de CDC vão para a fila morta — com o conector e o Kafka saudáveis, e o catálogo respondendo vazio |
| `cluster/dns-nodes.sh` | o resolvedor **dos nós**. Sem ele nem a imagem de sandbox chega, e todo pod fica em `Init:0/2` com um evento que fala de imagem, não de DNS |
| `cluster/import-images.sh` | o kubelet **coleta imagem "não usada"** acima de 85% de disco, e imagem carregada à mão não tem de onde voltar. Por isso elas são fixadas (`io.cri-containerd.pinned=true`) e conferidas uma a uma |
| `infra/register-cdc.sh` | o Kafka nasce vazio: sem registrar o conector, o catálogo fica vazio **para sempre**, com tudo saudável |

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
- **Nenhum HPA.** O terreno está pronto (metrics-server responde, todos os Deployments têm
  `requests.cpu`), mas três dos sete aplicativos têm impedimento antes: o relay da outbox da
  admin-api **pressupõe uma instância**, os tópicos de CDC têm uma partição só (réplica extra do
  catálogo fica ociosa no consumo), e o encoder pede escala **por profundidade de fila**, não por CPU.
- **Carregamento de imagem por tarball** é o ponto frágil: três cópias de 429 MB por recriação. O
  certo é um registro local do k3d, que exige recriar o cluster — por isso está documentado e não
  feito.
- **Sem cluster autoscaler**: três nós fixos, e a máquina com 31 GB. Aqui a escala serve para provar
  comportamento, não para ganhar capacidade.
