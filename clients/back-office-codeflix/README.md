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
├── categories/     tudo de categoria: o gateway, a consulta e a tela
├── shared/         o que não é de agregado nenhum: tema, layout, http, cliente de consultas
├── App.tsx         provedores e rotas
└── main.tsx        o ponto de entrada
```

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

## A lista de categorias

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

## As escolhas de ferramenta, e onde elas divergem do curso

A referência é o [FC3-admin-videos-react](https://github.com/devfullcycle/FC3-admin-videos-react). Ela
informa, não decide — e aqui divergiu em quatro pontos, todos por idade do que ela usa:

| O que ela usa | O que está aqui | Por quê |
|---|---|---|
| `react-scripts` 5.0.1 (CRA) | Vite 8 | o CRA foi descontinuado pelo próprio time do React |
| `@reduxjs/toolkit` 1.8 (RTK Query) | TanStack Query 5 | não há estado global a guardar: é cache de servidor |
| `@react-keycloak/web` 3.4 | `oidc-client-ts` + `react-oidc-context` | última publicação em **abril de 2022** |
| Material 5 e DataGrid 5 | Material 7.3 e DataGrid 8.29 | duas e três versões maiores atrás |

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

Existe: entrar, o guarda de administração, o layout com as quatro abas e a **lista** de categorias.

Não existe ainda: criar, editar e desativar categoria, e os outros três agregados — as abas estão lá
e dizem isso. Quando as telas de formulário entrarem, duas decisões ficam para aquele momento: a
biblioteca de formulário (a referência usa `react-hook-form` com `zod`) e o aviso de sucesso e erro
(ela usa `notistack`).

## O tamanho do pacote

As telas entram por `lazy`: o DataGrid é o pedaço mais pesado, e ninguém precisa dele para ver a
tela de entrar. Com isso o pacote de entrada caiu de 335 kB para **159 kB** comprimidos, e a tabela
chega depois do login.

O aviso de tamanho do Vite está em 700 kB, e não nos 500 kB padrão: os dois pedaços são Material e
DataGrid, que custam isto mesmo, e um aviso disparando em todo build treina a ignorar a saída. 700 kB
ainda grita se uma dependência nova entrar.

## Os testes

`npm test` roda o Vitest com jsdom. São 49, e cada guarda tem o seu: que o assinante não entra, que
a consulta espera o token, que `search` vazio não vai na URL, que 4xx não é repetido, que 401 e 403
dizem coisas diferentes. Todos foram conferidos ao contrário — removendo o que eles guardam e vendo
cada um falhar.

O DataGrid precisa de altura medida para desenhar linha, e o jsdom dá zero a tudo; por isso o teste
da tela finge `offsetHeight`. Sem isso a tabela existiria sem nenhuma célula, e o teste acusaria um
defeito que não existe.

**O que os testes não cobrem:** a volta do Keycloak. Trocar o `?code=` por token exige um navegador
de verdade; aqui o `useAuth` é dublado. É o mesmo limite registrado no portal.
