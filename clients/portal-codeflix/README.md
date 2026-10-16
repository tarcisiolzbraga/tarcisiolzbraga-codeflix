# portal-codeflix

O portal de quem assina: escolhe o plano, assina e assiste. É o primeiro cliente do monorepo, e fala
com a [`subscription-api`](../../services/subscription-api) e com a
[`videos-api`](../../services/videos-api).

## Como rodar

As APIs sobem como sempre, da pasta de cada uma. O portal sobe daqui:

```bash
npm install
npm run dev        # http://localhost:3000
```

`npm run check` é a porta de entrada de cada commit, como o `./gradlew build` dos serviços Java e o
`make check` dos Go: lint, tipos, testes e build, nessa ordem.

O `typecheck` roda `next typegen` antes do `tsc`, e não é zelo: tipos como `LayoutProps` e
`PageProps` são **gerados** pelo Next a partir das rotas, e num checkout novo eles não existem — o
`tsc` falhava com `Cannot find name 'LayoutProps'` até o primeiro build. Conferido apagando
`next-env.d.ts` e `.next` para simular o CI.

A telemetria do Next, que é ligada por padrão e manda dado de build para fora, fica desligada no CI
pela variável `NEXT_TELEMETRY_DISABLED`.

## De onde vem a configuração

**Do `.env` da raiz do monorepo, que é um só.** O [`next.config.ts`](next.config.ts) o lê com
`process.loadEnvFile` e deriva as URLs das APIs das portas que já estão lá
(`SUBSCRIPTION_APP_PORT`, `CATALOG_APP_PORT`, `APP_PORT`) — assim o dia em que uma porta mudar no
compose, o portal acompanha. É o mesmo arranjo que os serviços Java têm com o
`spring.config.import: optional:file:../../.env`, e é opcional do mesmo jeito: sem o arquivo, valem
as variáveis de ambiente de verdade.

O [`src/shared/config.ts`](src/shared/config.ts) é o equivalente do `ConfiguredValue` dos serviços
Java: confere a URL e **derruba** se ela vier vazia, com placeholder não resolvido ou relativa, em
vez de deixar o portal subir apontando para lugar nenhum.

> `NEXT_PUBLIC_*` é embutido no pacote **em tempo de build**, não lido em tempo de execução. Uma
> imagem construída para homologação não serve para produção — quando houver implantação em
> container, cada ambiente precisa do seu build.

## Como as pastas estão organizadas

`src/app/` é imposição do Next: é dele que saem as rotas. O resto é **por agregado**, como nos
serviços Java:

```
src/
├── app/            as rotas (Next manda aqui)
│   ├── page.tsx    a entrada
│   └── planos/     a tela de escolha
├── plans/          tudo de plano: tipos, a conversa com a API, e os componentes
└── shared/         o que não é de agregado nenhum
```

## As rotas são em português

Não é gosto: a `subscription-api` **já manda** o link de validação do cadastro para
`/cadastro/confirmar` (veja `amqp.confirmation-url`). O idioma das rotas estava decidido antes do
portal existir, e divergir dele quebraria o e-mail que já é enviado.

## O login

Authorization code + PKCE S256, contra o client público `front-codeflix` do
[realm](../../provisioning/keycloak/realm.json). O emissor e o nome do client vêm do **mesmo `.env`**
que os três serviços usam, montados das mesmas variáveis que o `keycloak.issuer-uri` deles — divergir
aqui faria o token do portal ser recusado pelo `iss`, que é justamente o defeito que o `.env` único
existe para evitar.

**`oidc-client-ts` + `react-oidc-context`, e não `next-auth` nem `keycloak-js`.** O motivo é
versão, e está medido:

| Biblioteca | Situação |
|---|---|
| `next-auth` v5 (o de App Router) | em **beta** há muito tempo |
| `next-auth` v4 | estável, mas é a API antiga |
| `keycloak-js` | 26.2.4, **atrás** do servidor do compose (26.8.0), e sem binding de React |
| `oidc-client-ts` 3.5.0 + `react-oidc-context` 3.3.1 | estáveis, padrão OIDC, hooks de React |

O token fica em **`sessionStorage`**, não em `localStorage`: ele morre quando a aba fecha, o que em
máquina compartilhada é a diferença entre sair e continuar logado para o próximo que abrir o browser.

`automaticSilentRenew` está ligado — sem ele, uma aba aberta por mais tempo que a validade do token
começa a receber 401 no meio da navegação.

### A renovação não é detalhe aqui

**O papel do catálogo só entra no token depois de renovar.** A assinatura concede
`CODEFLIX_SUBSCRIBER` por evento, alguns segundos depois do pagamento; o token que o portal tem na
mão foi assinado antes disso e não muda. Quando a tela de assinatura existir, ela vai precisar pedir
a renovação explicitamente depois de assinar — e é por ter esse controle explícito que a escolha foi
uma biblioteca OIDC, e não uma sessão de servidor.

## O que dá para verificar sem browser, e o que não dá

O login interativo não é automatizável aqui (não há browser de verdade na verificação), então o que
foi conferido contra a pilha no ar foi o que costuma estar errado:

- **o realm aceita o `redirect_uri` do portal com PKCE** — a chamada ao `authorization_endpoint` com
  `code_challenge_method=S256` devolve 200 e a tela de login, não "Invalid parameter: redirect_uri";
- **a verificação prévia de `GET /accounts/me` com o cabeçalho `Authorization` passa** — 200 com
  `Access-Control-Allow-Origin: http://localhost:3000`;
- **um 401 chega com cabeçalho de CORS**, então o front lê a recusa em vez de ver "erro de rede",
  que é o que acontece quando o CORS falta no caminho de erro.

O que **não** foi conferido de ponta a ponta: a volta do Keycloak com o código, a troca por token, e
a tela da conta com dados de verdade. Isso pede um browser, e está dito aqui em vez de subentendido.

## O que está pronto, e o que não está

Pronto: a tela de planos, lendo `GET /plans/on-sale`, que é **aberta** — quem está escolhendo plano
ainda não tem token, e é por isso que essa rota não pede um. E o login por Keycloak, com a tela da
conta lendo `GET /accounts/me` **com** token, que é a primeira que exercita o CORS de verdade.

A seguir, nesta ordem: o cadastro com confirmação por e-mail (a rota `/cadastro/confirmar` já é
esperada pela API), a assinatura, e o acervo pela `videos-api`.

## Duas coisas que vão morder quem continuar

**O papel do catálogo só chega ao token depois de renovar.** A assinatura concede
`CODEFLIX_SUBSCRIBER` por evento, alguns segundos depois do pagamento. O token que o portal já tem na
mão foi assinado antes disso e não muda: sem renovar, a tela do acervo responde 403 e parece defeito
da API.

**A busca dos planos é dinâmica, de propósito.** Com `cacheComponents` ligado, marcar a busca como
cacheada faria a página ser pré-renderizada no build — e aí **o build passaria a depender da API no
ar**. Aconteceu na primeira versão: `next build` tentou chamar a `subscription-api` e quebrou com
`ECONNREFUSED`. A lista vive dentro de um `<Suspense>`, então a casca é estática e só ela espera.

## Vitest, e não o Jest da referência

A referência do curso usa Jest. Com o Next 16 e o TypeScript atual, o Jest pede transformador e
configuração à parte; o Vitest lê o mesmo `tsconfig` e o mesmo alias `@/`.

**Componente de servidor assíncrono não é testável em nenhum dos dois** — o próprio Next recomenda
teste de ponta a ponta para isso. Então o que os testes cobrem é o que dá para cobrir de verdade: a
conversa com a API (incluindo o erro e o fato de a rota aberta não levar token), a formatação do
preço com a moeda que a API mandou, e a apresentação do cartão.

## As versões, e o teto do TypeScript

Tudo no mais atual publicado: Next 16.4.2, React 19.3.1, **TypeScript 6.0.3**, Tailwind 4, ESLint 10,
Vitest 5, Node 24 (fixado no [`.nvmrc`](.nvmrc), como o `go.mod` fixa o Go dos serviços em Go).

**O TypeScript 7 não entra, e não é escolha nossa.** O 7 é o compilador reescrito em nativo; o
`typescript-eslint`, que vem dentro do `eslint-config-next`, tem uma recusa explícita no código:

```js
if (versionMajor >= 7) {
  throw new Error('typescript-eslint does not support TS 7.0.');
}
```

Não é aviso: é `throw`, e o `npm run lint` para ali. O suporte é rastreado para TS >= 7.1 no
[issue 10940](https://github.com/typescript-eslint/typescript-eslint/issues/10940), e já há builds
`7.1.0-dev` sendo publicados — ou seja, isto se resolve por espera.

Por isso o teto é o **6.0.3**, que é a última linha estável do compilador em JavaScript e passa pela
checagem (ela só barra major >= 7). Conferido: lint, tipos, testes e build, todos verdes no 6.0.3.

O que custaria ir ao 7 hoje é trocar o ESLint por Biome, que não passa pelo `typescript-eslint` — e
com ele iriam as **22 regras específicas de Next** do `@next/eslint-plugin-next`, entre elas
`no-async-client-component`, `no-html-link-for-pages`, `no-img-element`, `no-sync-scripts` e
`no-typos` (que pega `getServerSideProps` escrito errado). Trocar essas regras por um número de
versão do compilador é um mau negócio enquanto a espera é curta.

## Uma vulnerabilidade conhecida, sem correção publicada

`npm audit` aponta cinco altas, e são **uma** causa só: `braces` ≤3.0.3, puxado por
`micromatch` → `fast-glob` → `@next/eslint-plugin-next` → `eslint-config-next`.

- é dependência **de desenvolvimento**: não vai para o pacote servido;
- a falha é negação de serviço por padrão de glob profundamente aninhado, e quem passa os padrões
  aqui é o nosso próprio lint;
- **3.0.3 é a última versão publicada**, então não há para onde subir: `overrides` não resolve, e
  `npm audit fix --force` só degradaria o `eslint-config-next`.

Fica registrado em vez de "consertado" às cegas. Quando sair correção, é só subir.
