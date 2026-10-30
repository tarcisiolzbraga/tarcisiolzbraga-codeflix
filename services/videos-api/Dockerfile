# Empacota o jar já construído: rodar ./gradlew bootJar antes do docker build.
FROM eclipse-temurin:25-jre-alpine

# Pega as correções de segurança publicadas **depois** de a imagem base ser construída, e isso não é
# zelo abstrato: o Trivy achou o CVE-2026-111497 no brotli-libs 1.2.0-r1 desta base, com correção
# publicada no 1.2.1-r0 e disponível no repositório do Alpine — a base é que ainda não foi
# reconstruída lá em cima. Sem esta linha, o conserto dependeria do calendário de outra pessoa.
#
# O preço é que o build deixa de ser reproduzível byte a byte: o resultado passa a depender do estado
# do repositório no momento. Para uma imagem de execução, é a troca certa — o alternativo é embarcar
# vulnerabilidade conhecida por amor à reprodutibilidade.
RUN apk -U upgrade --no-cache

# Usuário sem privilégio: a aplicação não tem motivo para rodar como root.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring build/libs/application.jar /opt/app/application.jar

USER spring:spring

# 8082 porque a 8080 é a do admin-codeflix e a 8081 é a do Keycloak dele.
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/opt/app/application.jar"]
