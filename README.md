# videos-api-codeflix

API de catálogo de vídeos do Codeflix, o lado que serve o usuário final. Lê o catálogo mantido pelo
`admin-codeflix` e o entrega ao cliente.

## Requisitos

- JDK 25
- Docker (para os testes de integração)

## Como rodar

```bash
./gradlew bootRun     # sobe a API
./gradlew build       # compila e testa os três módulos
./gradlew unitTests   # só os testes que dispensam Docker
```

## Arquitetura

Arquitetura limpa em três módulos Gradle, com as dependências apontando só para dentro:

- `domain`: Java puro, sem dependência de framework.
- `application`: casos de uso; depende só de `domain`.
- `infrastructure`: único módulo Spring Boot; depende dos outros dois.
