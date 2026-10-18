# back-office-codeflix

O painel de quem administra o catálogo: categorias, gêneros, elenco e vídeos. Fala com uma API só, a
[`admin-api`](../../services/admin-api) — é ela que manda no catálogo, e o resto do monorepo consome
o que ela publica.

O outro cliente, o [`portal-codeflix`](../portal-codeflix), é o lado de quem assina e assiste. Os
dois entram pelo **mesmo** client do Keycloak; quem separa o que cada um pode é o papel no token.

## Como rodar

A `admin-api` e o Keycloak sobem como sempre, das pastas deles. O back-office sobe daqui:

```bash
npm install
npm run dev        # http://localhost:3001
```

3001 e não 3000: o portal ocupa a 3000, e no desenvolvimento os dois rodam juntos. A porta está
liberada no `redirectUris` do client do [realm](../../provisioning/keycloak/realm.json), e é
`strictPort` — se estiver ocupada, o Vite falha em vez de subir numa porta que o Keycloak recusaria.

`npm run check` é a porta de entrada de cada commit, como o `./gradlew build` dos serviços Java e o
`make check` dos Go: lint, tipos, testes e build, nessa ordem.

### Com que conta entrar

O realm cria um humano para isto: **`operador-de-teste`**, com `CODEFLIX_ADMIN`. A senha vem do
`.env` da raiz, em `OPERATOR_TEST_PASSWORD`; sem a variável, o compose usa `operador-de-teste`.

Entrar com uma conta de assinante **funciona** — o client é o mesmo —, e o painel diz que falta
permissão em vez de mostrar a interface. O porquê está mais abaixo.

## De onde vem a configuração

**Do `.env` da raiz do monorepo, que é um só.** O [`vite.config.ts`](vite.config.ts) o lê com
`process.loadEnvFile` e deriva dele a URL da `admin-api` (de `APP_PORT`) e o emissor do Keycloak (de
`KEYCLOAK_HOST`, `KEYCLOAK_PORT` e `KEYCLOAK_REALM`) — as mesmas variáveis que montam o
`keycloak.issuer-uri` dos serviços. Divergir aqui faria o token ser recusado pelo `iss`, que é o
defeito que o `.env` único existe para evitar.

A substituição é por `define`, e não por `envDir` apontado para a raiz: o Vite só expõe variável com
prefixo `VITE_`, e o `.env` do monorepo não tem esse prefixo — ele descreve a infraestrutura, não
este cliente.

O [`src/shared/config.ts`](src/shared/config.ts) é o equivalente do `ConfiguredValue` dos serviços
Java: confere e **derruba** se a URL vier vazia ou não for absoluta, em vez de deixar o painel subir
apontando para lugar nenhum.

> Como no portal, o valor é embutido **em tempo de build**. Uma imagem construída para homologação
> não serve para produção.

## Como as pastas estão organizadas

Por agregado e papel, como nos serviços Java — não por camada técnica:

```
src/
├── auth/           entrar no Keycloak e o guarda de administração
├── categories/     tudo de categoria: o gateway, o esquema, as mutações e as telas
├── castMembers/    o mesmo, para o elenco
├── genres/         o mesmo, para os gêneros
├── videos/         o maior: vínculos, três estados e cinco arquivos
├── shared/         o que não é de agregado nenhum
├── App.tsx         provedores e rotas
└── main.tsx        o ponto de entrada
```

No `shared/` ficam o tema, o layout, o cliente da `admin-api`, o cliente de consultas, o aviso de
canto, o diálogo de confirmação, **a consulta de listagem** e **a tabela**. As duas últimas entraram
quando iam virar terceira cópia — o detalhe está em [A tabela que os agregados
dividem](#a-tabela-que-os-agregados-dividem).

## O login, e o guarda que vem depois dele

Authorization code + PKCE S256, contra o client público `front-codeflix` do realm, com
`oidc-client-ts` + `react-oidc-context` e token em `sessionStorage` — as mesmas escolhas do portal,
pelos mesmos motivos, que estão explicados [lá](../portal-codeflix/README.md#o-login). Num painel de
administração o `sessionStorage` vale ainda mais: é a diferença entre sair e deixar o catálogo aberto
para o próximo que usar a máquina.

O [`RequireAdmin`](src/auth/RequireAdmin.tsx) tem **dois degraus**, e o segundo é o que importa:

| Quem chega | O que vê |
|---|---|
| ninguém logado | o botão de entrar |
| assinante (sem `CODEFLIX_ADMIN`) | "sua conta não tem permissão", e o botão de sair |
| administrador | o painel |

O segundo degrau não é zelo. O client do Keycloak é o mesmo do portal, então um assinante **consegue
se autenticar aqui**. Sem a conferência ele veria a interface inteira e tomaria 403 em cada chamada
— o que parece defeito do sistema, e não falta de permissão.

**Conferir o papel no front não é a segurança.** Quem decide é a `admin-api`, em cada rota, e ela
decidiria igual se este arquivo não existisse. O guarda existe para a pessoa entender o que está
acontecendo.

O papel exigido é `CODEFLIX_ADMIN`, e não os por agregado (`CODEFLIX_CATEGORIES` e companhia): o
painel administra os quatro.

## As categorias

O CRUD completo: listar, criar, editar, ativar, desativar e remover.

### A lista

Paginação **e** ordenação no servidor, pelos mesmos parâmetros do `CategorySearchRequest` da
`admin-api` (`search`, `page`, `perPage`, `sort`, `dir`). Não é preferência: nenhuma rota de
listagem do monorepo devolve `List`, porque o catálogo não cabe no navegador.

Três coisas que o contrato impôs à tela:

- **`search` vazio não vai.** Do outro lado ele entra num `like`; mandar o parâmetro vazio é pedir à
  API um filtro que não se quer.
- **Filtrar volta para a primeira página.** Filtrar e continuar na página 4 mostraria "nenhum
  resultado" num filtro que tem resultados.
- **401 e 403 não são a mesma frase.** Parecem o mesmo defeito na tela e são opostos: no primeiro a
  sessão venceu e entrar de novo resolve; no segundo a conta não tem o papel e entrar de novo não
  muda nada. O mapa está no [`explainFailure`](src/shared/explainFailure.ts).

A busca é atrasada em 400 ms. O atraso é por tempo, e não `useDeferredValue`: o do React adia o
*render*, que não é o custo aqui — o custo é a chamada de rede.

O cliente de consultas **não repete 4xx**. O padrão do TanStack é três tentativas, e com isso um 403
vira três chamadas e segundos de espera antes de a pessoa ler "sem permissão", quando a primeira
resposta já disse tudo.

### Desativar não é remover, e a tela trata os dois diferente

| Ação | Pergunta antes? | Por quê |
|---|---|---|
| ativar / desativar | não | é reversível no mesmo botão |
| remover | **sim** | não há como desfazer |

E o caso que o monorepo decidiu desde o banco: **categoria referenciada não se apaga, se desativa**.
Nenhuma foreign key tem `ON DELETE CASCADE`, e a `admin-api` responde **409** quando a categoria está
em um gênero ou vídeo. A frase dela é em inglês (`is linked to at least one Genre; deactivate it
instead`), e aqui ela é traduzida — é um caso conhecido com uma ação só. Mensagem de **validação**
continua vindo como a API escreveu: lá a regra é dela, aqui o remédio é nosso.

### O interruptor de situação só aparece na criação

No `UpdateCategoryRequest` da `admin-api` o campo `active` **não existe**: ativar e desativar têm
rotas próprias. Mostrar o interruptor na edição prometeria uma mudança que a chamada não levaria.

A tela de edição carrega a categoria **pelo id**, em vez de reaproveitar a linha da tabela: a linha
pode estar velha, e salvar por cima do que outra pessoa mudou é pior que uma ida à rede.

### O formulário, e o que ele confere

`react-hook-form` com `zod`, as mesmas da referência do curso — aqui nas versões de hoje (7.90 e 4.6,
publicadas em outubro de 2026).

Ele confere o nome (3 a 255, contados depois de aparar) e o teto da descrição (4000). São os limites
do domínio, e repeti-los aqui não cria segunda fonte de verdade: cria um atalho para o caso comum,
porque a mensagem que volta da API é em inglês e o painel é em português. Quando a regra mudar lá, o
que chega é o 422, com a frase dela.

**O 422 mostra a lista inteira.** O `ApiError` da `admin-api` tem dois campos — `message`, que é a
primeira mensagem, e `errors`, que é todas. Um formulário com nome curto e descrição longa recebe
duas, e mostrar só a primeira faria a pessoa corrigir uma coisa e levar o mesmo erro de volta.

Descrição vazia vai como `null`, e não como string vazia: na coluna ela é anulável, e gravar `""`
faria a listagem mostrar uma célula vazia que o filtro `like` ainda alcança.

### O 500 que este trabalho encontrou na admin-api

A descrição da categoria não tinha teto **no domínio** — só na coluna, `VARCHAR(4000)`. Uma descrição
mais longa passava pela validação, quebrava no banco, e o erro de integridade não está mapeado no
`GlobalExceptionHandler`: a API respondia **500**, com corpo sem explicação nenhuma. Medido contra a
API de verdade antes de consertar.

O conserto foi no lugar certo, o domínio, e não neste formulário: o `CategoryValidator` agora recusa
o campo como o `VideoValidator` já fazia com a descrição do vídeo, e a resposta é 422. O corte no
formulário continua, pelo mesmo motivo que o do nome — é atalho, não remendo.

### O aviso de "salvo" e de "não deu"

`Snackbar` do Material, atrás de um contexto de trinta linhas. A referência usa `notistack`, e a
escolha aqui foi medida: a última publicação dele é de **janeiro de 2025**, e o que ele oferece além
disto é empilhar vários avisos — num painel a pessoa faz uma ação por vez.

O aviso de erro fica o dobro do tempo do de sucesso: ele costuma ser uma frase para ler, e o de
sucesso é uma palavra para conferir de canto de olho.

## O elenco

O mesmo CRUD da categoria, com uma diferença de contrato: em vez de descrição livre, um **papel
fechado** — `ACTOR` ou `DIRECTOR`, os dois valores do `CastMemberType` do domínio.

O campo é uma escolha, e não texto. A API aceita `diretor` em minúsculas (o `CastMemberType.of` usa
`equalsIgnoreCase`), mas aceitar texto livre aqui deixaria a pessoa digitar "atriz" e tomar um 422 em
inglês por algo que a tela sabia de antemão.

O papel aparece em **português na tela** e viaja em **maiúsculas na API**. A ordenação é por `type`,
que é o que está na coluna — ou seja, ordena por `ACTOR` e `DIRECTOR`, não pelo rótulo.

Ator é o padrão da criação: é o papel mais comum num catálogo, e o campo é obrigatório — sem padrão,
criar um ator exigiria uma escolha que não acrescenta nada.

O 409 aqui é o mesmo de lá, e por motivo parecido: **membro usado por algum vídeo não se apaga, se
desativa**.

## Os gêneros

O mesmo CRUD, com a primeira ligação entre agregados: um gênero tem **categorias**.

### O nome aceita uma letra, e isso não é descuido

O `GenreValidator` do domínio tem mínimo **1**, e não 3 como a categoria e o elenco. Copiar o 3 para
cá recusaria o que a API aceita — e o esquema foi escrito a partir do validador, não por analogia.

Gênero **sem categoria** também é estado válido: o `CreateGenreCommand` troca ausente por conjunto
vazio. Exigir uma aqui seria uma regra que a API não tem.

### A lista mostra a contagem, não os nomes

O `GenreListResponse` da `admin-api` traz as categorias como **ids**, e não há rota que receba vários
ids de uma vez. Mostrar nomes na tabela custaria uma chamada por categoria de **cada linha** — dez
linhas com três categorias seriam trinta requisições por página.

Então a coluna diz "3 categorias", e os nomes aparecem na tela de edição, onde a conta é limitada ao
gênero aberto. Um gênero sem nenhuma diz "0 categorias", em vez de ficar com a célula vazia.

### A escolha das categorias

Autocomplete com **busca no servidor**, como a da tabela e pelo mesmo motivo: o catálogo não cabe num
`<select>`. Vinte por busca basta para escolher digitando, e não é promessa de completude — quem não
vê o que quer digita mais.

Os ids que já vêm do gênero são resolvidos em nomes por uma consulta **por id**. Se um id não
resolver (categoria removida fora daqui, ou falha de rede), **o id continua aparecendo**: esconder a
etiqueta faria o gênero parecer ter menos categorias do que tem.

O campo é o mesmo que o vídeo usa para os três vínculos dele — veja [A tabela que os agregados
dividem](#a-tabela-que-os-agregados-dividem).

### Salvar substitui a lista inteira

`categories` no `PUT` **substitui**, não acrescenta — é o que o `UpdateGenreCommand` faz. Tirar a
última categoria manda lista vazia, que é como a API desvincula; mandar o campo ausente deixaria a
categoria lá.

## Os vídeos

O maior dos quatro, e o único com arquivo. Três coisas o separam dos outros.

### Três estados independentes, e seis rotas

`active`, `published` e `opened` são coisas diferentes, e nenhum passa pelo corpo da atualização:

| Estado | O que significa |
|---|---|
| `active` | o registro existe no catálogo |
| `published` | o vídeo aparece para quem navega |
| `opened` | pode ser assistido **sem assinatura** |

Cada um tem par de rotas (`publish`/`unpublish`, `open`/`close`, `activate`/`deactivate`). Na tela
eles são três interruptores, **com o que significam escrito ao lado** — e ficam fora da lista, de
propósito: `opened` libera o arquivo para quem não paga, e um interruptor ao lado de "remover"
convida ao clique errado.

Eles também salvam sozinhos, fora do botão "Salvar": cada um é uma chamada própria, e juntá-los ao
formulário faria parecer que nada aconteceu até salvar — quando a mudança já foi gravada.

### Os arquivos só entram depois de criar

A rota de envio é `/videos/{id}/medias/{type}`, então sem o vídeo criado não há onde pôr o arquivo.
A tela de criação **diz isso** em vez de mostrar um campo que falharia, e ao terminar vai para a
**edição** do vídeo criado, e não para a lista: é lá que os arquivos entram, e criar um vídeo sem
mídia é metade do trabalho.

São cinco: vídeo, trailer, banner, miniatura e miniatura meia. Os dois primeiros trazem um `status`
de codificação que os de imagem não têm — um vídeo recém-enviado fica em `PENDING` até o encoder
responder pela fila.

### O envio não passa pelo cliente comum, e o teto é conferido antes

O `adminFetch` põe `Content-Type: application/json` em toda chamada com corpo. Aqui o cabeçalho tem
de ser `multipart/form-data` **com o boundary**, que só o browser gera — definir o `Content-Type` à
mão faria o Spring recusar o corpo por não achar o boundary, e o erro não diria isso.

O teto vem do **`MEDIA_MAX_FILE_SIZE` do `.env` da raiz**, o mesmo valor que o
`spring.servlet.multipart.max-file-size` da `admin-api` usa, convertido para bytes no `vite.config`.
Ele é conferido **antes** de enviar porque a API responde **413 sem corpo nenhum** — medido contra a
API de verdade. Sem a conferência, a pessoa esperaria o upload inteiro para ler "falhou" sem motivo.

### Dois limites lidos do domínio, e não copiados

O `VideoValidator` não é o `CategoryValidator`:

| Campo | Vídeo | Categoria |
|---|---|---|
| título / nome | 1 a 255 | 3 a 255 |
| sinopse / descrição | **obrigatória**, até 4000 | opcional, até 4000 |

E a ordenação padrão é por **`title`**, não por `name`: é o único dos quatro agregados que difere, e
copiar `name` pediria ordenação por uma coluna que não existe.

Duração zero é válida (`duration < 0` é a única regra), e o ano não tem teto: catálogo tem filme de
1902, e estreia marcada para o ano que vem é caso real.

### Os filtros por vínculo

Só a rota de vídeo os tem. Eles vão como **repetição do mesmo parâmetro**
(`categories=a&categories=b`), que é como o Spring os lê num `Set<String>` — juntar com vírgula daria
um id só, inexistente.

## A tabela que os agregados dividem

`search`, `page`, `perPage`, `sort` e `dir` são iguais nos quatro `*SearchRequest` da `admin-api`.
Eles ficam no [`shared/search.ts`](src/shared/search.ts), e a tabela — campo de busca, aviso de
falha, paginação e ordenação no servidor — no
[`shared/AggregateTable.tsx`](src/shared/AggregateTable.tsx).

Isto não estava no primeiro desenho, e entrou **quando ia virar a terceira cópia**. As duas primeiras
ainda cabiam em cada tela; a terceira já seria a mesma decisão mantida em três lugares — a de não
trazer `List` do banco, que é a razão pela qual nenhuma rota de listagem da `admin-api` devolve lista
inteira.

As colunas continuam em cada tela, inclusive a de ações: os rótulos e os avisos são diferentes, e
genericizá-los esconderia a diferença em vez de a documentar.

O **campo de escolha** seguiu o mesmo caminho, e pelo mesmo critério: ele nasceu para as categorias
de um gênero, e o vídeo precisava de três. Os três recursos escolhíveis — categoria, gênero e elenco
— têm a mesma forma no que importa (id e nome), então o
[`ReferencePicker`](src/shared/ReferencePicker.tsx) recebe o recurso e serve aos quatro usos.

A chave de cache de um registro lido por id é **a mesma** que a tela de edição dele usa. A rota é a
mesma (`GET /{recurso}/{id}`) e devolve o mesmo corpo, então abrir um gênero depois de abrir uma
categoria não refaz a chamada.

O que a extração custou e rendeu, medido no build: os pedaços de cada agregado caíram para **1 a 3
kB**, porque DataGrid e formulário agora são pedaço comum em vez de estarem dentro de cada tela.

## As escolhas de ferramenta, e onde elas divergem do curso

A referência é o [FC3-admin-videos-react](https://github.com/devfullcycle/FC3-admin-videos-react). Ela
informa, não decide — e aqui divergiu em cinco pontos, todos por idade do que ela usa:

| O que ela usa | O que está aqui | Por quê |
|---|---|---|
| `react-scripts` 5.0.1 (CRA) | Vite 8 | o CRA foi descontinuado pelo próprio time do React |
| `@reduxjs/toolkit` 1.8 (RTK Query) | TanStack Query 5 | não há estado global a guardar: é cache de servidor |
| `@react-keycloak/web` 3.4 | `oidc-client-ts` + `react-oidc-context` | última publicação em **abril de 2022** |
| Material 5 e DataGrid 5 | Material 7.3 e DataGrid 8.29 | duas e três versões maiores atrás |
| `notistack` 3.0.2 | `Snackbar` do Material | parado desde janeiro de 2025, e empilhar avisos não é preciso aqui |

O que **não** divergiu: `react-hook-form` com `zod` para o formulário. Os dois estão em dia (7.90 e
4.6, de outubro de 2026), e a escolha dela continua certa.

Material **7**, e não 9: o `@mui/x-data-grid` 8 não aceita Material 9 como par, e o DataGrid 9 só
existe como *release candidate*. 7.3 com 8.29 é o par estável mais novo que existe hoje — medido, não
suposto.

**oxlint, e não ESLint.** O linter é em Rust e não carrega o `typescript-eslint`, o que libera o
TypeScript 6 (o `typescript-eslint` recusa versão que não conhece). As duas regras que pegam defeito
de verdade — `react/set-state-in-effect` e `react-hooks/exhaustive-deps` — estão promovidas a
`error` no [`.oxlintrc.json`](.oxlintrc.json), e não aviso.

**`react-router` 8, e não `react-router-dom`.** Desde a 7 o pacote `-dom` é só um reexportador.

O `tsconfig` não declara `strict`: no TypeScript 6 ele é o padrão. Conferido — um parâmetro sem tipo
e um acesso a possível `null` falham a compilação.

## O que já existe, e o que não

**Os quatro agregados por inteiro**: categorias, elenco, gêneros e vídeos — listar, criar, editar,
ativar, desativar e remover, mais os três estados do vídeo e o envio dos cinco arquivos.

O que **não** existe, e é escolha: o player. Rever a mídia enviada é trabalho do portal, e o
back-office mostra o nome do arquivo e o status da codificação, que é o que quem administra precisa
saber. A `admin-api` tem a rota de download (com `Range`), então isto é adicionável.

## O tamanho do pacote

As telas entram por `lazy`: o DataGrid é o pedaço mais pesado, e ninguém precisa dele para ver a
tela de entrar. Com isso o pacote de entrada ficou em **82 kB** comprimidos — era 335 kB quando tudo
vinha junto —, e a tabela chega depois do login.

O aviso de tamanho do Vite está em 700 kB, e não nos 500 kB padrão: os dois pedaços são Material e
DataGrid, que custam isto mesmo, e um aviso disparando em todo build treina a ignorar a saída. 700 kB
ainda grita se uma dependência nova entrar.

## Os testes

`npm test` roda o Vitest com jsdom. São 236, e cada guarda tem o seu: que o assinante não entra, que
a consulta espera o token, que `search` vazio não vai na URL, que 4xx não é repetido, que 401 e 403
dizem coisas diferentes, que `active` não vai no corpo da atualização, que remover pergunta antes,
que o 409 manda desativar, que o papel do elenco é fechado nos dois valores do domínio, que a rota
dele leva hífen, que o nome do gênero aceita uma letra, que a lista de gêneros mostra contagem e não
ids, que tirar a última categoria manda lista vazia, que o vídeo ordena por `title`, que o filtro
repete o parâmetro em vez de juntar com vírgula, que a sinopse é obrigatória, que o teto do arquivo é
conferido antes de enviar, que o envio **não** define `Content-Type`, e que cada estado do vídeo vai
na rota dele. Todos foram conferidos ao contrário — removendo o que eles guardam e vendo
cada um falhar.

Um deles ficou **verde** nessa conferência, e vale registrar o que ensinou: estreitar o tipo do corpo
da atualização (`Pick<CategoryDraft, "name" | "description">`) é documentação, não guarda — quem
mantém o `active` fora da chamada é a tela de edição, que monta o objeto campo por campo. O teste
passou a mirar nela.

Os testes de tela usam o cliente de consultas **de produção**, e não um com `retry: false`: um dublê
ali faria os testes passarem com uma política que ninguém usa. O preço é que falha de rede levaria as
repetições em tempo real, e por isso aquele caso é testado no `queryClient.test.ts`, sem tela.

O DataGrid precisa de altura medida para desenhar linha, e o jsdom dá zero a tudo; por isso o teste
da tela finge `offsetHeight`. Sem isso a tabela existiria sem nenhuma célula, e o teste acusaria um
defeito que não existe.

**O que os testes não cobrem:** a volta do Keycloak. Trocar o `?code=` por token exige um navegador
de verdade; aqui o `useAuth` é dublado. É o mesmo limite registrado no portal.
