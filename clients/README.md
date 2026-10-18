# Os clientes

Esta pasta guarda os fronts que falam com as APIs de [`services/`](../services/). São dois:

| Cliente | Para quem | Com quem fala | Porta |
|---|---|---|---|
| [`portal-codeflix/`](portal-codeflix/) | quem assina e assiste | `subscription-api` e `videos-api` | 3000 |
| [`back-office-codeflix/`](back-office-codeflix/) | quem administra o catálogo | `admin-api` | 3001 |

Os dois entram pelo **mesmo** client do Keycloak, o `front-codeflix`. Ele é "por onde uma pessoa
entra"; quem separa o que cada um pode é o **papel** no token — `CODEFLIX_ACCOUNT` e
`CODEFLIX_SUBSCRIBER` de um lado, `CODEFLIX_ADMIN` do outro. Um client por front exigiria manter duas
listas de `redirectUris` sem ganhar controle algum, já que os dois são públicos.

Cadeias de ferramenta diferentes, de propósito: o portal é Next (renderiza no servidor, e isso
importa para quem chega pelo Google), o back-office é Vite (não renderiza nada no servidor, porque
tudo atrás dele exige login). Os dois expõem o mesmo `npm run check`, e é só isso que o CI precisa
saber.

A diferença aparece na imagem: o portal carrega um servidor Node e deu **286 MB**; o back-office é
pacote estático num nginx e deu **90 MB**.

## As URLs são decididas no build, e isso vale para os dois

`NEXT_PUBLIC_*` no portal e o `define` do Vite no back-office fazem a mesma coisa: **substituem a URL
no código em tempo de build**. Então ela entra como `build-arg`, e não como variável de ambiente do
container — passá-la no `run` não mudaria nada, porque o valor já está no JavaScript que o browser
baixa.

| no `.env` da raiz | o que acontece |
|---|---|
| `PUBLIC_*` vazias | cada URL cai para `http://localhost:<porta>`, o padrão de desenvolvimento |
| `PUBLIC_*` preenchidas | a URL inteira é usada, e a porta é ignorada |

A primeira linha é o que a máquina de quem programa quer: as portas das APIs são publicadas no host,
e é o **browser** que as alcança, não o container do front.

A consequência, dita antes de alguém descobrir: **uma imagem construída para um ambiente não serve
para outro.** É o preço de um front que não lê configuração em tempo de execução, e a alternativa —
servir a configuração por uma rota e buscá-la ao abrir — custa uma ida à rede antes da primeira tela.

## Por que separado de services/

É o mesmo argumento do [README da raiz](../README.md#por-que-num-repositório-só) para o monorepo
inteiro, só um nível abaixo: o que divergiria entre repositórios separados são os nomes de papel
(`CODEFLIX_ACCOUNT` e `CODEFLIX_SUBSCRIBER`), as rotas, e as URLs de cada ambiente — e divergência
dessas três não aparece em teste nenhum de um lado só.

A separação entre as duas pastas é de **cadeia de ferramentas**, não de importância: `services/` é
Gradle e Go, `clients/` é Node. Cada um entra no CI com o seu próprio job, como já acontece entre os
serviços Java e os Go.

## O que o cliente precisa saber da API

Está escrito, e não vale copiar para cá:

- **as origens aceitas por ambiente** e o que CORS resolve (e o que não resolve): a seção
  [O front, e o CORS](../README.md#o-front-e-o-cors);
- **o fluxo de autenticação**: o client `front-codeflix` do
  [realm](../provisioning/keycloak/realm.json) é público, com authorization code + PKCE S256;
- **o contrato de cada API**: o README de cada serviço, e o Swagger UI no ar em `development`;
- **um detalhe que custa uma tarde**: o papel do catálogo (`CODEFLIX_SUBSCRIBER`) é concedido por
  evento depois da assinatura, alguns segundos depois. O token que o front já tem na mão foi assinado
  antes disso e **não muda** — sem renovar o token, a tela do catálogo responde 403 e parece defeito
  da API.
