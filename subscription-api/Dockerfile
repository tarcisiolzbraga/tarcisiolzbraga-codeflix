# Empacota o jar já construído: rodar ./gradlew bootJar antes do docker build.
FROM eclipse-temurin:25-jre-alpine

# Usuário sem privilégio: a aplicação não tem motivo para rodar como root.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring build/libs/application.jar /opt/app/application.jar

USER spring:spring

# 8084 porque a 8080 é da admin-api, a 8081 é do Keycloak, a 8082 é da videos-api e a 8083 é a API
# REST do Kafka Connect.
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/opt/app/application.jar"]
