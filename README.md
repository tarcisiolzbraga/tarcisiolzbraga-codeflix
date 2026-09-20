# admin-codeflix

Administração do catálogo de vídeos do Codeflix. Java 25 + Spring Boot 4.1.1 + Gradle, em arquitetura limpa.

## Módulos
- `domain`: entidades, value objects e regras de negócio (Java puro).
- `application`: casos de uso, depende apenas do `domain`.
- `infrastructure`: Spring Boot (API REST, JPA, Flyway, MySQL).

## Execução
Copie o `.env.example` para `.env` e preencha usuário e senha do banco. O `docker compose` e a aplicação leem as variáveis desse arquivo.

```bash
cp .env.example .env
docker compose up -d
./gradlew bootRun
```

### Perfis
| Perfil | Quando | Configuração do banco | Swagger UI |
|---|---|---|---|
| `development` | padrão, sem perfil ativo (`bootRun`, testes) | `.env`, com `localhost` como padrão | sim |
| `homolog` | ambiente de homologação | variáveis de ambiente (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) | sim |
| `production` | produção | variáveis de ambiente, todas obrigatórias | não |

O tamanho do pool de conexões vem de `DB_POOL_SIZE` (padrão 5 no `development` e 20 nos outros). O perfil é escolhido por `SPRING_PROFILES_ACTIVE`:

```bash
SPRING_PROFILES_ACTIVE=homolog java -jar build/libs/application.jar
```

Com a aplicação no ar, a documentação da API (Swagger UI) fica em http://localhost:8080/swagger-ui.html, e o JSON do OpenAPI em http://localhost:8080/v3/api-docs.

## Testes
```bash
./gradlew test
```

O `./gradlew build` também gera o relatório de cobertura (JaCoCo) dos três módulos em `build/reports/jacoco/html/index.html`.
