package com.tarcisiolzbraga.codeflix.admin.e2e.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.e2e.CastMemberE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@E2ETest
class CastMemberE2ETest implements CastMemberE2EDsl {

    private static final String VIN_DIESEL = "Vin Diesel";
    private static final String SPIELBERG = "Steven Spielberg";

    @Value("${local.server.port}")
    private int port;

    private RestClient client;

    @Autowired
    private KeycloakTestToken token;

    @BeforeEach
    void setUpClient() {
        // Toda chamada leva o token: a API exige role, e o E2E é o único lugar que prova isso contra
        // um emissor de verdade.
        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .build();
    }

    @Override
    public RestClient client() {
        return this.client;
    }

    @Test
    void givenNoCastMember_whenCreateAnActor_thenItCanBeRetrievedWithItsType() {
        final var id = givenACastMember(VIN_DIESEL, "ACTOR");

        final var actualCastMember = retrieveACastMember(id);

        assertEquals(VIN_DIESEL, actualCastMember.name());
        assertEquals("ACTOR", actualCastMember.type());
        assertTrue(actualCastMember.active());
        assertEquals(actualCastMember.createdAt(), actualCastMember.updatedAt());
    }

    @Test
    void givenAnUnknownTypeAndNoName_whenCreate_thenReturnUnprocessableContentWithBothErrors() {
        final var request = new CreateCastMemberRequest(null, "SINGER", null);

        final var actualException = assertThrows(
                HttpClientErrorException.class, () -> createACastMember(request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actualException.getStatusCode());
        final var apiError = actualException.getResponseBodyAs(ApiError.class);
        assertEquals(
                List.of("'name' should not be null", "'type' should be one of ACTOR, DIRECTOR"), apiError.errors());
    }

    @Test
    void givenThreeCastMembers_whenListTheSecondPage_thenReturnOnlyTheSecondByName() {
        givenACastMember(VIN_DIESEL, "ACTOR");
        givenACastMember(SPIELBERG, "DIRECTOR");
        givenACastMember("Ana Silva", "ACTOR");

        final var actualPage = listCastMembers(1, 1);

        assertEquals(3, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals(SPIELBERG, actualPage.items().getFirst().name());
    }

    @Test
    void givenThreeCastMembers_whenSearchByATermOfTheName_thenReturnOnlyTheMatch() {
        givenACastMember(VIN_DIESEL, "ACTOR");
        givenACastMember(SPIELBERG, "DIRECTOR");
        givenACastMember("Ana Silva", "ACTOR");

        final var actualPage = listCastMembers(0, 10, "spiel");

        assertEquals(1, actualPage.total());
        assertEquals(SPIELBERG, actualPage.items().getFirst().name());
    }

    @Test
    void givenAnActor_whenUpdateItsNameAndType_thenTheApiReturnsTheNewValues() {
        final var id = givenACastMember("Vin", "ACTOR");

        updateACastMember(id, VIN_DIESEL, "DIRECTOR");

        final var actualCastMember = retrieveACastMember(id);
        assertEquals(VIN_DIESEL, actualCastMember.name());
        assertEquals("DIRECTOR", actualCastMember.type());
    }

    @Test
    void givenAnActiveCastMember_whenDeactivateIt_thenItKeepsExistingInactive() {
        final var id = givenACastMember(VIN_DIESEL, "ACTOR");

        final var actualResponse = deactivateACastMember(id);

        assertFalse(actualResponse.active());
        assertFalse(retrieveACastMember(id).active());
    }

    @Test
    void givenADeactivatedCastMember_whenActivateIt_thenItIsActiveAgain() {
        final var id = givenACastMember(VIN_DIESEL, "ACTOR");
        deactivateACastMember(id);

        final var actualResponse = activateACastMember(id);

        assertTrue(actualResponse.active());
        assertTrue(retrieveACastMember(id).active());
    }

    @Test
    void givenACastMember_whenDeleteIt_thenItIsGone() {
        final var id = givenACastMember(VIN_DIESEL, "ACTOR");

        deleteACastMember(id);

        final var actualException =
                assertThrows(HttpClientErrorException.class, () -> retrieveACastMember(id));
        assertEquals(HttpStatus.NOT_FOUND, actualException.getStatusCode());
        assertEquals(0, listCastMembers(0, 10).total());
    }
}
