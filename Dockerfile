# Empacota o jar já construído: rodar ./gradlew bootJar antes do docker build.
FROM eclipse-temurin:25-jre-alpine

# Usuário sem privilégio: a aplicação não tem motivo para rodar como root.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring build/libs/application.jar /opt/app/application.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/app/application.jar"]
