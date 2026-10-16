# Os clientes

Esta pasta guarda os fronts que falam com as APIs de [`services/`](../services/). Está aqui vazia de
propósito no commit que criou o layout: o primeiro cliente, o portal do assinante, entra a seguir.

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
