package com.tarcisiolzbraga.codeflix.admin.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtConverterTest {

    private static final String SUBJECT = "service-account-admin-codeflix";

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Test
    void givenRealmRoles_whenConvert_thenPrefixThemWithRole() {
        final var jwt = jwtWith(Map.of("realm_access", Map.of("roles", List.of("CODEFLIX_ADMIN"))));

        final var actualToken = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_ADMIN"), authorityNames(actualToken.getAuthorities()));
    }

    @Test
    void givenLowercaseRole_whenConvert_thenUpperCaseIt() {
        final var jwt = jwtWith(Map.of("realm_access", Map.of("roles", List.of("codeflix_videos"))));

        final var actualToken = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_VIDEOS"), authorityNames(actualToken.getAuthorities()));
    }

    @Test
    void givenClientRoles_whenConvert_thenQualifyThemWithTheClientName() {
        final var jwt = jwtWith(Map.of(
                "resource_access", Map.of("admin-codeflix", Map.of("roles", List.of("READER")))));

        final var actualToken = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_ADMIN-CODEFLIX_READER"), authorityNames(actualToken.getAuthorities()));
    }

    @Test
    void givenBothKinds_whenConvert_thenKeepAllOfThem() {
        final var jwt = jwtWith(Map.of(
                "realm_access", Map.of("roles", List.of("CODEFLIX_ADMIN")),
                "resource_access", Map.of("outro", Map.of("roles", List.of("WRITER")))));

        final var actualToken = this.converter.convert(jwt);

        assertEquals(
                Set.of("ROLE_CODEFLIX_ADMIN", "ROLE_OUTRO_WRITER"), authorityNames(actualToken.getAuthorities()));
    }

    @Test
    void givenNoRoleClaim_whenConvert_thenReceiveNoAuthority() {
        final var actualToken = this.converter.convert(jwtWith(Map.of()));

        assertTrue(actualToken.getAuthorities().isEmpty());
    }

    @Test
    void givenAMalformedRoleClaim_whenConvert_thenIgnoreIt() {
        final var jwt = jwtWith(Map.of("realm_access", Map.of("roles", "não é lista")));

        final var actualToken = this.converter.convert(jwt);

        assertTrue(actualToken.getAuthorities().isEmpty());
    }

    @Test
    void givenAJwt_whenConvert_thenTheSubjectBecomesThePrincipal() {
        final var actualToken = this.converter.convert(jwtWith(Map.of()));

        assertEquals(SUBJECT, actualToken.getName());
    }

    private Jwt jwtWith(final Map<String, Object> claims) {
        final var builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(SUBJECT)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }

    private Set<String> authorityNames(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
    }
}
