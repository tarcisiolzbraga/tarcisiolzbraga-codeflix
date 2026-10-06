# Realm de desenvolvimento

`realm.json` é importado pelo Keycloak na subida do `docker compose`, então o container entrega o realm
`codeflix` pronto: as seis roles e quatro clients de credenciais de cliente.

**Não é possível comentar dentro do arquivo.** Diferente do import de definitions do RabbitMQ, o Keycloak recusa
campo desconhecido no realm — um `_comment` derruba a subida com `Unrecognized field`. Por isso estas notas
estão aqui.

## Os quatro clients

| Client | Role | Para quê |
|---|---|---|
| `admin-codeflix` | `CODEFLIX_ADMIN` | acesso a todo o catálogo |
| `categories-codeflix` | `CODEFLIX_CATEGORIES` | exercitar a restrição: nas rotas dos outros agregados ele recebe 403 |
| `videos-api-codeflix` | as quatro de leitura | a API de catálogo, que replica os quatro agregados |
| `subscriber-codeflix` | `CODEFLIX_SUBSCRIBER` | ler o catálogo **do outro lado**, na `videos-api-codeflix` |

O `videos-api-codeflix` existe porque o `categories-codeflix` **não** serve a esse propósito: ele é a peça que
prova o 403, e usá-lo como credencial do catálogo o faria perder essa função. O do catálogo leva só as quatro
roles de leitura, nunca a de administrador — ele lê e não escreve nada aqui.

O `subscriber-codeflix` e a role `CODEFLIX_SUBSCRIBER` não têm uso nesta API: aqui eles só recebem 403, porque
toda rota pede `CODEFLIX_ADMIN` ou a role do agregado. Eles estão neste arquivo porque o realm é um só, e é a
`videos-api-codeflix` que os consome — é com eles que um assinante lê o catálogo lá. A role de leitura de um
agregado **não** foi reaproveitada para isso: `CODEFLIX_CATEGORIES` significa poder administrar categorias
nesta API, e emprestá-la ao catálogo juntaria duas permissões diferentes numa só.

Os três compartilham o mesmo segredo porque este realm é de desenvolvimento, local e descartável.

## O segredo

O arquivo traz `__KEYCLOAK_CLIENT_SECRET__` no lugar do segredo. O Keycloak **não** substitui variável de
ambiente dentro do import — testado com `${env.X}` e com propriedade de sistema, e nos dois casos o texto do
placeholder é gravado como se fosse o próprio segredo. A troca acontece antes da leitura: no `docker compose`,
pelo `command` do serviço, com o valor de `KEYCLOAK_CLIENT_SECRET`; nos testes, pelo
`KeycloakContainerConfiguration`, que lê este mesmo arquivo e usa um segredo fixo.

Ou seja, este arquivo é a única descrição do realm, e vale tanto para desenvolvimento quanto para os testes —
as roles e os clients não podem divergir entre os dois.
