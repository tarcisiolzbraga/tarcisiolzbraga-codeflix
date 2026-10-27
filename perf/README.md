# Teste de carga

Antes disto, nada no projeto sabia quanto ele aguenta. Agora sabe — com ressalvas que estão escritas
aqui, porque número sem ressalva é número que alguém vai citar fora de contexto.

```bash
python3 perf/semear.py 200     # cria e publica 200 vídeos pelo caminho real (admin → CDC → catálogo)
bash perf/degraus.sh           # mede nível por nível e monta a tabela
bash perf/rodar.sh             # uma rampa só, com foto das métricas do actuator antes e depois
```

Contra o cluster, os três aceitam os endereços por ambiente:

```bash
CATALOGO_URL=http://catalogo.localtest.me KEYCLOAK_URL=http://keycloak.localtest.me \
  bash perf/degraus.sh
```

## O que é medido, e por quê

A **leitura paginada do catálogo** — `videos(page, perPage)` no GraphQL da `videos-api`, com
`categories`, `genres` e `castMembers` resolvidos. É o caminho de maior volume num produto de
streaming: a escrita é de operador, e o acervo é de todo mundo.

O token é pedido uma vez no `setup()` do k6, cujo tempo não entra nas medidas, com o client
`catalog-test-codeflix` — que tem `CODEFLIX_SUBSCRIBER`, o mesmo papel de um assinante de verdade.

## A ressalva que vem antes dos números

**O gerador de carga divide a máquina com o que ele mede.** A medição abaixo saiu de um k3d de três
nós com 18 pods e sidecar do Istio em cada um, com o k6 ao lado. Então:

- os números **não** são capacidade de produção. Numa máquina dedicada seriam outros;
- o que é válido é a **comparação** — entre formas da mesma consulta, e entre níveis de carga. O
  joelho e a razão entre as duas formas não mudam por o vizinho ser barulhento.

## O que foi medido

Catálogo com **201 vídeos**, cada um com 3 categorias, 3 gêneros e 3 do elenco. Páginas de 10,
sorteadas entre as 20 primeiras a cada iteração — com página fixa o cache responderia sempre o mesmo
e o número sairia bonito pelo motivo errado.

### A curva, com as relações resolvidas

| VUs | req/s | mediana | p95 | p99 | erro |
|---:|---:|---:|---:|---:|---:|
| 1 | 96 | 8,8ms | 17,5ms | 28,3ms | 0% |
| 5 | 322 | 12,9ms | 21,6ms | 26,6ms | 0% |
| 10 | **701** | 12,1ms | 17,3ms | 20,9ms | 0% |
| 20 | **769** | 23,2ms | 30,8ms | 37,0ms | 0% |
| 40 | 723 | 52,0ms | 65,4ms | 75,4ms | 0% |
| 80 | 578 | 129,8ms | 207,4ms | 242,9ms | 0% |

**O joelho está entre 10 e 20 VUs.** A vazão chega a ~770 req/s e depois **cai**: 723 com 40 VUs,
578 com 80. Passado o joelho, acrescentar concorrência piora as duas coisas — é saturação, não
limite de teto.

**Zero erro em todos os níveis.** O serviço degrada; não quebra. Vale dizer porque é o oposto do que
se teme num teste de carga, e porque significa que o limite é de latência, não de falha.

### O que custa: as relações

A mesma consulta sem `categories`, `genres` e `castMembers`:

| forma | p99 com 1 VU | vazão no pico | joelho |
|---|---:|---:|---|
| com relações | 28,3ms | **769 req/s** | 10–20 VUs |
| sem relações | 19,9ms | **1.870 req/s** | 20–40 VUs |

**Resolver as relações custa 2,4× da vazão**, e empurra o joelho de 10–20 para 20–40 VUs.

### E não é N+1

Era a minha primeira hipótese, e o código a derrubou: o
[`VideoRelationsGraphQLController`](../services/videos-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/videos/infrastructure/graphql/VideoRelationsGraphQLController.java)
já usa `@BatchMapping` nas três. São **três** `findAllById` por página, não três por vídeo.

O custo é essas três idas extras ao Elasticsearch por requisição — mais a serialização de um corpo
muito maior (835 MB recebidos em 95.801 requisições, ~8,7 KB por resposta).

Então o caminho **não** é batch, que já existe. É um destes dois:

- **cache de curta duração** nas três buscas por id. Categoria, gênero e elenco mudam raramente, e um
  cache de segundos colapsaria quase todo esse custo;
- **desnormalizar o nome** no documento do vídeo. O índice já guarda os ids; guardar os nomes
  elimina as três buscas, ao preço de reindexar quando um nome muda.

### O GC era 7% do tempo, e virou 1,4%

A primeira medição deixou uma pista: muita pausa de GC. Investigada, a causa não era a aplicação —
era a JVM não saber o tamanho do container:

```
MaxHeapSize      = 268435456  (256 MB de 1 GiB)   {ergonomic}
MaxRAMPercentage = 25.0                           {default}
UseSerialGC      = true                           {ergonomic}
pools: "Eden Space" / "Survivor Space" / "Tenured Gen"
processo: java -jar /opt/app/application.jar      (nenhuma flag)
```

Duas coisas de uma vez: **768 MB do limite não eram usados pelo heap**, e o coletor era
**stop-the-world de uma thread**. A escolha do serial é por **memória**, não por CPU — abaixo de
1792 MB disponíveis a JVM classifica o container como "client class", e a máquina tem 16 CPUs
visíveis sem limite de cgroup. Por isso `-XX:+UseG1GC` precisa ser explícito: subir só o percentual
deixaria o coletor como estava.

Com `-XX:MaxRAMPercentage=70 -XX:+UseG1GC` nos três serviços Java, medido no catálogo com 10 VUs e
50 s, duas execuções consecutivas na mesma máquina:

| | antes | depois |
|---|---:|---:|
| pausas de GC | 1.209 | **490** |
| tempo total em GC | 3.651 ms | **698 ms** |
| pausa média | 3,0 ms | **1,4 ms** |
| **% do tempo em pausa** | **7,3%** | **1,4%** |
| média no servidor | 11,9 ms | **9,7 ms** |
| requisições em 50 s | 30.637 | **37.461** |

E o efeito na curva de saturação, nos mesmos degraus de antes:

| VUs | antes | depois |
|---|---:|---:|
| 10 | 701 req/s | **790** |
| 20 | 769 req/s | **869** |
| 40 | 723 req/s | **899** |

**O colapso depois do joelho desapareceu.** Antes a vazão *caía* ao passar de 20 VUs — 769 para 723
— enquanto a latência crescia; agora ela continua subindo. O joelho segue entre 10 e 20 VUs, mas o
que vem depois dele deixou de ser perda. O p99 a 40 VUs caiu de 75,4 para 62,3 ms.

Duas ressalvas honestas:

- **70%, e não 75 ou 80**: sobram ~300 MB para metaspace, cache de código, pilhas e buffers diretos.
  Errar para cima troca pausa de GC por `OOMKill`, que é pior;
- **a medição é do catálogo**, e as flags foram para os três serviços Java pelo mesmo diagnóstico —
  os três rodavam `java -jar` sem flag, com o mesmo limite de 1 GiB. A `admin-api` e a
  `subscription-api` não foram medidas sob carga.

### O que o serviço sentiu

Da rampa de 80 VUs, pelo `/actuator/prometheus` antes e depois:

```
média no servidor: 40,8 ms por requisição (95.800 medidas)
GC: 2.732 pausas somando 13.897 ms (5,1 ms cada)
heap: 86 MB -> 155 MB
threads vivas: 149 -> 149
```

Dois fatos que isso dá:

- **a latência está no serviço, não numa fila antes dele**: 40,8ms no servidor contra 43,5ms no
  cliente. Os 2,7ms de diferença são rede e enfileiramento — ou seja, não há gargalo de aceitação;
- **as virtual threads fazem o que prometem**: 149 threads antes e depois, com 80 VUs em cima. O teto
  não é de thread. Vale registrar porque a suspeita natural num serviço Spring seria essa.

O GC merece atenção numa próxima volta: 13,9s de pausa em ~170s de teste é cerca de 8% do tempo, com
limite de 1 GiB de heap no manifesto.

## Os limites são de correção, não de latência

O k6 reprova se houver **qualquer** falha de transporte ou erro de GraphQL (`http_req_failed: rate==0`
e `checks: rate==1`). Não há teto de latência, e isso é escolha: exigir p99 só faria sentido num nível
fixo e numa máquina dedicada — aqui reprovaria por causa do vizinho, não do código.

## O que não existe

- **não roda no CI.** Teste de carga em runner compartilhado mede o runner. Entra no dia em que
  houver uma máquina para isso;
- **só o caminho de leitura do catálogo.** A escrita no admin, o envio de mídia e o cadastro na
  assinatura não foram medidos. O da assinatura tem uma pegadinha: o limitador de requisição dela é
  de 120/min por origem, então um teste de um host só mediria o limitador a partir de 2 req/s;
- **nada sobre o streaming de mídia**, que é onde vive a intermitência documentada. Esse teste é o
  candidato natural a reproduzi-la sob demanda, e seria a próxima volta.
