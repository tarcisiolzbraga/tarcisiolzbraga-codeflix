# Realm de desenvolvimento

Este arquivo fica em `provisioning/`, e não dentro de um serviço, porque o realm é um só e os três
serviços Java o usam, com papéis diferentes: a `admin-api` e a `videos-api` validam token nele, e a
`subscription-api` além disso **administra os usuários** dele.

`realm.json` é importado pelo Keycloak na subida do `docker compose` — o serviço está no
`docker-compose.yml` ao lado —, então o container entrega o realm
`codeflix` pronto: as seis roles, cinco clients de credenciais de cliente e o grupo de assinantes.

**Não é possível comentar dentro do arquivo.** Diferente do import de definitions do RabbitMQ, o Keycloak recusa
campo desconhecido no realm — um `_comment` derruba a subida com `Unrecognized field`. Por isso estas notas
estão aqui.

## Os cinco clients

| Client | Role | Para quê |
|---|---|---|
| `admin-codeflix` | `CODEFLIX_ADMIN` | acesso a todo o catálogo |
| `categories-codeflix` | `CODEFLIX_CATEGORIES` | exercitar a restrição: nas rotas dos outros agregados ele recebe 403 |
| `videos-api-codeflix` | as quatro de leitura | a API de catálogo, que replica os quatro agregados |
| `subscriber-codeflix` | `CODEFLIX_SUBSCRIBER` | ler o catálogo **do outro lado**, na `videos-api-codeflix` |
| `subscription-api-codeflix` | `manage-users` e `view-users` do `realm-management` | criar e procurar usuário, para o cadastro da assinatura |

O `videos-api-codeflix` existe porque o `categories-codeflix` **não** serve a esse propósito: ele é a peça que
prova o 403, e usá-lo como credencial do catálogo o faria perder essa função. O do catálogo leva só as quatro
roles de leitura, nunca a de administrador — ele lê e não escreve nada aqui.

O `subscriber-codeflix` e a role `CODEFLIX_SUBSCRIBER` não têm uso na `admin-api`: lá eles só recebem 403,
porque toda rota pede `CODEFLIX_ADMIN` ou a role do agregado. Eles estão neste arquivo porque o realm é um só, e é a
`videos-api-codeflix` que os consome — é com eles que um assinante lê o catálogo lá. A role de leitura de um
agregado **não** foi reaproveitada para isso: `CODEFLIX_CATEGORIES` significa poder administrar categorias
na `admin-api`, e emprestá-la ao catálogo juntaria duas permissões diferentes numa só.

Todos compartilham o mesmo segredo porque este realm é de desenvolvimento, local e descartável.

## O client que administra usuários

O `subscription-api-codeflix` é o único com poder sobre o realm, e por isso é client próprio em vez de
roles acrescentadas a um que já existia. O `subscriber-codeflix` seria o candidato óbvio, e não serve:
ele representa **o assinante**, e é a role dele que a `videos-api` confere para servir o catálogo —
emprestá-lo ao serviço faria a credencial de uma pessoa qualquer poder criar e ler todos os usuários do
realm.

As roles não são do realm, são **do client interno `realm-management`**, que é como o Keycloak expõe a
API administrativa. Só as duas de que o cadastro precisa:

- `manage-users` — criar o usuário, pô-lo no grupo e marcar `emailVerified` na confirmação;
- `view-users` — procurar por e-mail, para saber se já existe.

Nenhuma role de administrar o realm, os clients ou as roles. Verificado contra o container: com este
client o `POST /admin/realms/codeflix/users` responde 201, a busca por e-mail devolve o usuário, o
`PUT .../groups/{id}` responde 204, e os outros quatro clients seguem recebendo as roles de antes.

## O grupo de assinantes

O grupo `subscribers` carrega a role `CODEFLIX_SUBSCRIBER`, e é nele que a conta entra quando nasce. A
role continua existindo solta — é ela que o token precisa ter —, e o grupo é só a forma de concedê-la
sem mexer em role de usuário um por um.

Ele está aqui antes do código que o usa: o arquivo é compartilhado pelos três serviços, e mexer nele
duas vezes custa mais do que uma configuração esperando o seu consumidor.

## O segredo

O arquivo traz `__KEYCLOAK_CLIENT_SECRET__` no lugar do segredo. O Keycloak **não** substitui variável de
ambiente dentro do import — testado com `${env.X}` e com propriedade de sistema, e nos dois casos o texto do
placeholder é gravado como se fosse o próprio segredo. A troca acontece antes da leitura: no `docker compose`,
pelo `command` do serviço, com o valor de `KEYCLOAK_CLIENT_SECRET`; nos testes, pelo
`KeycloakContainerConfiguration`, que lê este mesmo arquivo e usa um segredo fixo.

Ou seja, este arquivo é a única descrição do realm, e vale tanto para desenvolvimento quanto para os testes —
as roles e os clients não podem divergir entre os dois.
