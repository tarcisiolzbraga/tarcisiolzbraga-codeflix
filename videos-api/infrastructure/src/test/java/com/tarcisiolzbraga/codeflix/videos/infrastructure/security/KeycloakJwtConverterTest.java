package com.tarcisiolzbraga.codeflix.videos.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

// Lógica pura, então teste sem Spring: o que está em jogo é a tradução das claims do Keycloak para
// o formato que o @Secured entende.
class KeycloakJwtConverterTest {

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Test
    void givenRealmRoles_whenConvert_thenTurnThemIntoPrefixedAuthorities() {
        final var jwt = aJwt(Map.of("realm_access", Map.of("roles", List.of("CODEFLIX_SUBSCRIBER"))));

        final var actualToken = converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_SUBSCRIBER"), namesOf(actualToken.getAuthorities()));
    }

    @Test
    void givenARoleInLowerCase_whenConvert_thenUpperCaseIt() {
        final var jwt = aJwt(Map.of("realm_access", Map.of("roles", List.of("codeflix_subscriber"))));

        final var actualToken = converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_SUBSCRIBER"), namesOf(actualToken.getAuthorities()));
    }

    // A role do client é qualificada pelo nome dele: duas roles homônimas de clients diferentes não
    // podem virar a mesma autoridade.
    @Test
    void givenClientRoles_whenConvert_thenQualifyThemWithTheClientName() {
        final var jwt = aJwt(Map.of("resource_access", Map.of("catalogo", Map.of("roles", List.of("leitor")))));

        final var actualToken = converter.convert(jwt);

        assertEquals(Set.of("ROLE_CATALOGO_LEITOR"), namesOf(actualToken.getAuthorities()));
    }

    @Test
    void givenRealmAndClientRoles_whenConvert_thenBringBothTogether() {
        final var jwt = aJwt(Map.of(
                "realm_access", Map.of("roles", List.of("CODEFLIX_ADMIN")),
                "resource_access", Map.of("catalogo", Map.of("roles", List.of("leitor")))));

        final var actualToken = converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_ADMIN", "ROLE_CATALOGO_LEITOR"), namesOf(actualToken.getAuthorities()));
    }

    @Test
    void givenATokenWithoutRoleClaims_whenConvert_thenGrantNothing() {
        final var jwt = aJwt(Map.of());

        final var actualToken = converter.convert(jwt);

        assertTrue(actualToken.getAuthorities().isEmpty());
    }

    // Claim corrompida é possível: o token vem de fora, e um realm_access sem a lista de roles não
    // pode derrubar a requisição com ClassCastException.
    @Test
    void givenARoleClaimThatIsNotAList_whenConvert_thenIgnoreItInsteadOfFailing() {
        final var jwt = aJwt(Map.of("realm_access", Map.of("roles", "CODEFLIX_SUBSCRIBER")));

        final var actualToken = converter.convert(jwt);

        assertTrue(actualToken.getAuthorities().isEmpty());
    }

    @Test
    void givenAToken_whenConvert_thenUseTheSubjectAsThePrincipal() {
        final var jwt = aJwt(Map.of());

        final var actualToken = converter.convert(jwt);

        assertEquals("a1b2", actualToken.getName());
    }

    private static Set<String> namesOf(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private static Jwt aJwt(final Map<String, Object> claims) {
        final var builder = Jwt.withTokenValue("token").header("alg", "none").subject("a1b2");
        claims.forEach(builder::claim);
        return builder.build();
    }
}
