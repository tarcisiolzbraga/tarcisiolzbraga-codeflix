# codeflix

Os serviços de back-end do Codeflix, num repositório só. Cada serviço tem o seu build, o seu
`Dockerfile` e o seu README; o que é de todos fica na raiz.

| Pasta | O que é | Porta |
|---|---|---|
| [`admin-api/`](admin-api/) | administra o catálogo: cria, edita e publica. É a fonte do dado. | 8080 |
| [`videos-api/`](videos-api/) | serve o catálogo ao usuário final, replicando o que a `admin-api` publica. | 8082 |
| `encoder-api/` | 🚧 ainda não existe: converte as mídias enviadas. | — |

A leitura de cada um está no README da própria pasta. Comece pelo
[`admin-api/README.md`](admin-api/README.md) se quer entender de onde o dado vem, ou pelo
[`videos-api/README.md`](videos-api/README.md) se quer entender como ele é servido.

## Por que num repositório só

Os serviços não são independentes, e tratá-los como tal custava caro:

- **O realm é um só.** Dar um papel ao assinante do catálogo era mudança em dois repositórios, e a
  `videos-api` mantinha uma cópia mínima do realm só para os testes dela, que podia divergir da de
  verdade. Hoje [`.keycloak/realm.json`](.keycloak/) é o único, e os testes dos dois leem dele.
- **O contrato do CDC atravessa os dois.** O nome do tópico está na configuração da `videos-api`, o
  conector está na `admin-api`, e as mensagens de teste da `videos-api` foram capturadas da saída
  real da `admin-api`. Renomear uma tabela de um lado quebra o outro, e agora um push só roda as
  duas suítes.
- **A infraestrutura é compartilhada.** MySQL, Keycloak, Kafka e RabbitMQ servem aos dois.

## Como rodar

Cada serviço sobe a partir da própria pasta, e é de lá que o Gradle roda:

```bash
cd admin-api  && ./gradlew build     # ou videos-api
```

A infraestrutura ainda está em dois `docker-compose.yml`, um por serviço — unificá-los na raiz é o
próximo passo. Até lá, siga o README de cada um: o da `videos-api` depende do Kafka e do Keycloak que
o compose da `admin-api` sobe.

## Integração contínua

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) é um workflow só, com os dois serviços Java
numa matriz: testes sem container, build completo com Testcontainers, e a imagem Docker de cada um.

**Não há filtro por caminho, de propósito.** Filtrar teste pelo que mudou daria push verde com
contrato quebrado: mexer só na `admin-api` quebra a `videos-api`, que replica o dado dela. O filtro
entra no dia em que houver deploy — e aí no deploy, não nos testes.

## Histórico

Os dois serviços vinham de repositórios separados, e a junção **não** reescreveu o histórico: os 364
commits dos dois lados entraram intactos, com as mensagens, as datas e as assinaturas que já tinham.
Como o `git log` ordena por data, o histórico aparece entrelaçado — a `admin-api` começou em 17/09 e
a `videos-api` em 02/10, e os dias em que as duas andaram juntas se leem como um dia só de trabalho.
