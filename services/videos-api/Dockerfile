# Empacota o jar já construído: rodar ./gradlew bootJar antes do docker build.
FROM eclipse-temurin:25-jre-alpine

# Usuário sem privilégio: a aplicação não tem motivo para rodar como root.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring build/libs/application.jar /opt/app/application.jar

USER spring:spring

# 8082 porque a 8080 é a do admin-codeflix e a 8081 é a do Keycloak dele.
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/opt/app/application.jar"]
