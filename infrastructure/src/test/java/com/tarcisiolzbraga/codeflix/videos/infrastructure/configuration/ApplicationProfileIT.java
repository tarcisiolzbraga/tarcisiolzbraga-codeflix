package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.core.env.Environment;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// Cada perfil é um contexto à parte: só o que muda entre eles é testado aqui.
//
// As três propriedades abaixo são as que o application.yml exige sem valor padrão, e que em homolog
// e produção vêm do ambiente. Declará-las aqui faz duas coisas: deixa o teste rodar sem o .env, e
// registra no código qual é o conjunto mínimo que a plataforma precisa fornecer. Os endereços do
// Elasticsearch e do Kafka não entram porque os containers já os publicam.
class ApplicationProfileIT {

    private static final String ADMIN_API = "ADMIN_API_BASE_URL=http://localhost:8080";
    private static final String TOKEN_URI =
            "KEYCLOAK_TOKEN_URI=http://localhost:8081/realms/codeflix/protocol/openid-connect/token";
    private static final String CLIENT_SECRET = "KEYCLOAK_CLIENT_SECRET=segredo-de-teste";

    private static final String INTROSPECTION = "{ __schema { queryType { name } } }";

    @Nested
    @IntegrationTest
    @TestPropertySource(properties = {ADMIN_API, TOKEN_URI, CLIENT_SECRET})
    class Development {

        @Autowired
        private Environment environment;

        @Test
        void givenNoActiveProfile_whenStartApp_thenUseDevelopment() {
            final var profileMatches = this.environment.matchesProfiles("development");

            assertTrue(profileMatches);
        }

        @Test
        void givenDevelopmentProfile_whenReadLogging_thenWriteTheFileInEcs() {
            final var actualFormat = this.environment.getProperty("logging.structured.format.file");

            assertEquals("ecs", actualFormat);
        }

        @Test
        void givenDevelopmentProfile_whenReadGraphiql_thenServeTheConsole() {
            final var actualEnabled = this.environment.getProperty("spring.graphql.graphiql.enabled");

            assertEquals("true", actualEnabled);
        }
    }

    @Nested
    @IntegrationTest
    @ActiveProfiles("homolog")
    @TestPropertySource(properties = {ADMIN_API, TOKEN_URI, CLIENT_SECRET})
    class Homolog {

        @Autowired
        private Environment environment;

        @Test
        void givenHomologProfile_whenReadLogging_thenWriteTheConsoleInEcs() {
            assertEquals("ecs", this.environment.getProperty("logging.structured.format.console"));
            assertNull(this.environment.getProperty("logging.file.name"));
        }

        @Test
        void givenHomologProfile_whenReadGraphiql_thenKeepTheConsoleForWhoValidates() {
            final var actualEnabled = this.environment.getProperty("spring.graphql.graphiql.enabled");

            assertEquals("true", actualEnabled);
        }
    }

    @Nested
    @IntegrationTest
    @AutoConfigureGraphQlTester
    @ActiveProfiles("production")
    @TestPropertySource(properties = {ADMIN_API, TOKEN_URI, CLIENT_SECRET})
    class Production {

        @Autowired
        private Environment environment;

        @Autowired
        private GraphQlTester graphQlTester;

        @Test
        void givenProductionProfile_whenReadGraphiql_thenTakeTheConsoleOffTheAir() {
            final var actualEnabled = this.environment.getProperty("spring.graphql.graphiql.enabled");

            assertEquals("false", actualEnabled);
        }

        // O console desligado é uma página a menos; o que esconde o contrato é a introspecção, e é
        // ela que este teste exercita de verdade, pedindo o schema como o console pediria.
        @Test
        void givenProductionProfile_whenAskForTheSchemaByIntrospection_thenRefuseIt() {
            final var response = this.graphQlTester.document(INTROSPECTION).execute();

            response.errors().satisfy(errors -> assertFalse(errors.isEmpty()));
        }
    }
}
