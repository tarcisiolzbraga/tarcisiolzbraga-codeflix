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

## O cadastro

Duas telas, e as duas carregam decisão da API:

**`/cadastro`** pede o cadastro. A API responde **202 com a mesma mensagem em todos os casos** —
e-mail novo, e-mail que já existe no provedor de identidade, ou e-mail que já tem conta. É decisão
dela, para não contar a um estranho se um endereço está cadastrado, e **a tela preserva isso**: o
sucesso é o mesmo texto, sem "este e-mail já existe".

**`/cadastro/confirmar`** é o endereço que o link do e-mail abre. O nome da rota não foi escolhido
aqui: a `subscription-api` manda este endereço desde antes de o portal existir, no
`amqp.confirmation-url` dela. Divergir quebraria o e-mail que já é enviado hoje.

### O token sai da URL assim que é lido

A API recebe o token **no corpo**, não em query string, porque em query string ele acabaria no log do
servidor — e ele é credencial. O portal o recebe na URL porque chega por link de e-mail, o que é
inevitável; o que dá para fazer é tirá-lo de lá no primeiro instante, com `history.replaceState`, para
ele não ficar no histórico do navegador nem viajar em `Referer`.

Isso tem uma consequência no código: o token é capturado na **primeira** renderização, porque logo
depois o parâmetro desaparece. Sem isso a tela voltaria para "falta o link" justamente depois de
confirmar.

### O que o portal não valida

**Os dígitos verificadores do documento.** A API confere CPF e CNPJ pela conta dos dígitos, não só
pelo comprimento; repetir isso aqui seria uma segunda fonte de verdade para a mesma regra, e o dia em
que ela mudasse no domínio o portal passaria a recusar o que a API aceita. O formulário confere campo
vazio e o tamanho da senha (8 a 72, que é o limite do domínio) e deixa o resto para a resposta da API,
mostrando **a mensagem que ela escreveu**.

## A assinatura

Assinar nasce **em período de experiência e sem cobrança**: cobrar é ato separado na API, e o portal
não finge o contrário. A tela de `/assinatura` mostra a situação, deixa pagar, cancelar e guardar o
meio de pagamento.

### A renovação do token, que aqui é a parte que importa

Depois de `POST /subscriptions`, o botão chama `signinSilent()` **antes** de navegar. Não é zelo: a
assinatura concede `CODEFLIX_SUBSCRIBER` por evento, alguns segundos depois, e o token que está na
mão foi assinado antes disso — ele não muda sozinho. Sem essa linha, a tela do acervo responderia 403
e pareceria defeito da API.

É por precisar desse controle que a escolha foi uma biblioteca OIDC no browser, e não uma sessão de
servidor.

### Três coisas que o contrato impõe à tela

- **Cartão recusado não é erro de HTTP.** `POST /subscriptions/me/charge` responde **200** mesmo
  quando o provedor recusa: a requisição foi atendida, e o que aconteceu está no corpo, com o
  motivo. Tratar como erro esconderia a razão da recusa.
- **Nunca ter assinado não é erro.** `GET /subscriptions/me` responde 404 para quem nunca assinou —
  o estado normal de quem acabou de criar conta. O gateway devolve `null` e a tela mostra o caminho
  para os planos, em vez de um aviso vermelho.
- **Cancelar o que já está cancelado não é erro**, por contrato.

### Não há campo de número de cartão, e isso é de propósito

O que a API guarda é **a referência do provedor**: ela recebe um token de uso único, troca por uma
referência durável e guarda só a referência. Num provedor de verdade, o token vem do SDK dele no
browser. Dado de cartão não passa nem pelo portal nem pelo serviço — e por isso a tela pede o token
do provedor, e diz na própria tela que é isso que ela está pedindo.

## O acervo

Busca e página de título, pelo GraphQL da `videos-api` — em `/api/graphql`, porque o contexto `/api`
faz parte do endereço dela.

### A recusa que vira "renove a sessão"

**O catálogo recusa dentro do corpo, com HTTP 200**, e a classificação vem em
`errors[0].extensions.classification`. Isso muda o tratamento:

| Caso | HTTP | Corpo | O que a tela faz |
|---|---|---|---|
| sem token | 200 | `UNAUTHORIZED` | pede para entrar |
| token sem o papel do catálogo | 200 | `FORBIDDEN` | **oferece renovar a sessão** |
| token com o papel | 200 | dados | mostra o acervo |
| token inválido | **401** | — | erro de transporte |

O `FORBIDDEN` quase nunca é "não pode assistir": é o papel que **ainda não chegou ao token**, porque
a assinatura o concede por evento, segundos depois. Dizer "acesso negado" a quem acabou de pagar
seria mentir; a tela oferece renovar, que é o que resolve.

### A reprodução, e o endereço assinado

O player aponta para um endereço **assinado** que o catálogo emite no clique — não para um arquivo
baixado antes.

**Por que não dá para apontar direto para a rota com token:** `<video src>` não carrega cabeçalho de
`Authorization`. O browser busca o `src` por conta própria, sem token. A primeira versão desta tela
contornava isso baixando os bytes com `fetch` e tocando de um endereço de objeto local; funcionava, e
custava a reprodução progressiva.

Com o endereço assinado a reprodução começa na hora, e a autorização continua num lugar só: a
assinatura é emitida pela `videos-api` depois de conferir direito, vale **cinco minutos** e não
concede nada além daquele arquivo. O caminho vem relativo, e quem junta com a base é o portal — quem
sabe o host público do catálogo é quem publica o serviço.

O endereço é pedido **no clique**, e não ao abrir a página: a assinatura é curta, e pedi-la antes
faria ela vencer na mão de quem leu a sinopse com calma.

**A busca na linha de tempo funciona**: o `<video>` pede trecho com `Range`, o catálogo repassa e a
admin-api serve 206. O player não precisou de nada para isso — quem faz o pedido de trecho é o
browser.

## O que dá para verificar sem browser, e o que não dá

O login interativo não é automatizável aqui (não há browser de verdade na verificação), então o que
foi conferido contra a pilha no ar foi o que costuma estar errado:

- **o realm aceita o `redirect_uri` do portal com PKCE** — a chamada ao `authorization_endpoint` com
  `code_challenge_method=S256` devolve 200 e a tela de login, não "Invalid parameter: redirect_uri";
- **a verificação prévia de `GET /accounts/me` com o cabeçalho `Authorization` passa** — 200 com
  `Access-Control-Allow-Origin: http://localhost:3000`;
- **um 401 chega com cabeçalho de CORS**, então o front lê a recusa em vez de ver "erro de rede",
  que é o que acontece quando o CORS falta no caminho de erro.

Do cadastro, o laço inteiro **foi** conferido pelo lado da API, com a pilha no ar:

- a verificação prévia de `POST /accounts/sign-up` da origem do portal passa, e o 202 vem com
  `Access-Control-Allow-Origin`;
- pedir duas vezes o mesmo e-mail devolve resposta **idêntica** — a propriedade de privacidade se
  mantém;
- **o link que a API enfileira é `http://localhost:3000/cadastro/confirmar?token=…`**, isto é, bate
  com a rota criada aqui;
- confirmar devolve 200 com o `accountId`, e confirmar **de novo com o mesmo token** devolve a mesma
  conta: idempotente, como o contrato promete;
- depois de pedir o cadastro outra vez, o link **antigo** responde 404 e o novo 200 — "só o link mais
  novo vale", provado de propósito.

Das rotas da assinatura, o que dá para conferir sem token de pessoa é o alcance pelo browser, e ele
foi conferido: a verificação prévia de `POST /subscriptions`, `PUT /subscriptions/me/cancel`,
`POST /subscriptions/me/charge` e `PUT /accounts/me/payment-method` responde 200 com
`Access-Control-Allow-Origin`, e as recusas sem token chegam **com** cabeçalho de CORS — o front lê
o 401 em vez de ver erro de rede.

Do acervo, os cinco caminhos foram exercitados contra a `videos-api` no ar — a verificação prévia em
`/api/graphql` (200 com `Access-Control-Allow-Origin`, o contexto `/api` não atrapalha), a recusa sem
token (200 + `UNAUTHORIZED`), a recusa por falta do papel (200 + `FORBIDDEN`), o acesso com o papel
(dados) e o token inválido (401). São exatamente os cinco que o gateway distingue.

O que **não** foi conferido de ponta a ponta: a volta do Keycloak com o código, a troca por token, o
laço de assinar → evento → papel no token renovado, e as telas com dados de verdade. Tudo isso pede
um browser e uma pessoa logada; do lado do backend, esse laço é coberto pelos testes de ponta a ponta
da `subscription-api`. Está dito aqui em vez de subentendido.

## O que está pronto, e o que não está

Pronto: a tela de planos, lendo `GET /plans/on-sale`, que é **aberta** — quem está escolhendo plano
ainda não tem token, e é por isso que essa rota não pede um. E o login por Keycloak, com a tela da
conta lendo `GET /accounts/me` **com** token, que é a primeira que exercita o CORS de verdade.

O cadastro, com a confirmação por e-mail. E a assinatura: assinar a partir do plano, ver a
situação, pagar, cancelar e guardar o meio de pagamento.

E o acervo: busca e página de título, pelo GraphQL da `videos-api`.

O que falta para o portal fazer o que o nome promete é **reprodução**, e ela depende de uma rota de
mídia que a `videos-api` ainda não tem — está explicado abaixo.

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
