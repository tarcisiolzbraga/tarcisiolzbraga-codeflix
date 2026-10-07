package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

// Prova que o realm da raiz subiu como o esperado. Sem isto, uma importação que falhasse em silêncio
// apareceria depois como "autorização recusada" em todos os testes de segurança, e o motivo
// verdadeiro — o papel que não foi concedido — ficaria escondido.
@IntegrationTest
class KeycloakTestTokenIT {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Autowired
    private KeycloakTestToken token;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void givenTheSubscriberClient_whenAskForAToken_thenCarryTheSubscriberRole() {
        final var actualRoles = realmRolesIn(this.token.subscriber());

        assertEquals(List.of("CODEFLIX_SUBSCRIBER"), actualRoles);
    }

    @Test
    void givenTheAdminClient_whenAskForAToken_thenCarryTheAdminRole() {
        final var actualRoles = realmRolesIn(this.token.admin());

        assertTrue(actualRoles.contains("CODEFLIX_ADMIN"));
    }

    // O desconhecido prova a recusa, e prova mais do que ausência de papel: ele tem um papel do
    // realm, só não um que valha aqui. Sem isso, um teste de 403 poderia passar por acidente, com o
    // token sendo recusado por não ter papel nenhum.
    @Test
    void givenTheStrangerClient_whenAskForAToken_thenCarryARoleThatDoesNotCountHere() {
        final var actualRoles = realmRolesIn(this.token.bearerOf(KeycloakTestToken.STRANGER));

        assertTrue(actualRoles.contains("CODEFLIX_CATEGORIES"));
        assertFalse(actualRoles.contains("CODEFLIX_SUBSCRIBER"));
        assertFalse(actualRoles.contains("CODEFLIX_ADMIN"));
    }

    // Lê a carga do token sem validar: aqui o que se confere é o que o Keycloak pôs nela, e a
    // validação da assinatura é assunto do resource server, não deste teste.
    private List<String> realmRolesIn(final String bearer) {
        final var payload = bearer.replace("Bearer ", "").split("\\.")[1];
        final var json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        final var claims = this.mapper.readValue(json, Map.class);
        if (!(claims.get(REALM_ACCESS) instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }
        return realmAccess.get(ROLES) instanceof List<?> roles
                ? roles.stream().map(String::valueOf).filter(role -> role.startsWith("CODEFLIX_")).toList()
                : List.of();
    }
}
