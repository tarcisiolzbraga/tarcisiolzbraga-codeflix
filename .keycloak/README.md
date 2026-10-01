# Realm de desenvolvimento

`realm.json` é importado pelo Keycloak na subida do `docker compose`, então o container entrega o realm
`codeflix` pronto: as cinco roles e dois clients de credenciais de cliente.

**Não é possível comentar dentro do arquivo.** Diferente do import de definitions do RabbitMQ, o Keycloak recusa
campo desconhecido no realm — um `_comment` derruba a subida com `Unrecognized field`. Por isso estas notas
estão aqui.

## Os dois clients

| Client | Role | Para quê |
|---|---|---|
| `admin-codeflix` | `CODEFLIX_ADMIN` | acesso a todo o catálogo |
| `categories-codeflix` | `CODEFLIX_CATEGORIES` | exercitar a restrição: nas rotas dos outros agregados ele recebe 403 |

Os dois compartilham o mesmo segredo porque este realm é de desenvolvimento, local e descartável.

## O segredo

O arquivo traz `__KEYCLOAK_CLIENT_SECRET__` no lugar do segredo. O Keycloak **não** substitui variável de
ambiente dentro do import — testado com `${env.X}` e com propriedade de sistema, e nos dois casos o texto do
placeholder é gravado como se fosse o próprio segredo. A troca acontece antes da leitura: no `docker compose`,
pelo `command` do serviço, com o valor de `KEYCLOAK_CLIENT_SECRET`; nos testes, pelo
`KeycloakContainerConfiguration`, que lê este mesmo arquivo e usa um segredo fixo.

Ou seja, este arquivo é a única descrição do realm, e vale tanto para desenvolvimento quanto para os testes —
as roles e os clients não podem divergir entre os dois.
