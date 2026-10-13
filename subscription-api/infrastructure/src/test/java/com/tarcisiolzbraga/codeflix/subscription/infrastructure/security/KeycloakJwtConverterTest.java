package com.tarcisiolzbraga.codeflix.subscription.infrastructure.security;

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

    private static final String SUBJECT = "service-account-subscriber-codeflix";
    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Test
    void givenRealmRoles_whenConverts_thenPrefixesThemWithRole() {
        final var jwt = jwtWith(Map.of(REALM_ACCESS, Map.of(ROLES, List.of("CODEFLIX_SUBSCRIBER"))));

        final var actual = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_SUBSCRIBER"), authorityNames(actual.getAuthorities()));
    }

    @Test
    void givenLowerCaseRole_whenConverts_thenUpperCasesIt() {
        final var jwt = jwtWith(Map.of(REALM_ACCESS, Map.of(ROLES, List.of("codeflix_admin"))));

        final var actual = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_ADMIN"), authorityNames(actual.getAuthorities()));
    }

    @Test
    void givenClientRoles_whenConverts_thenQualifiesThemWithTheClientName() {
        final var jwt = jwtWith(Map.of(RESOURCE_ACCESS, Map.of("subscriber-codeflix", Map.of(ROLES, List.of("READER")))));

        final var actual = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_SUBSCRIBER-CODEFLIX_READER"), authorityNames(actual.getAuthorities()));
    }

    @Test
    void givenBothKinds_whenConverts_thenKeepsAllOfThem() {
        final var jwt = jwtWith(Map.of(
                REALM_ACCESS,
                Map.of(ROLES, List.of("CODEFLIX_ADMIN")),
                RESOURCE_ACCESS,
                Map.of("outro", Map.of(ROLES, List.of("WRITER")))));

        final var actual = this.converter.convert(jwt);

        assertEquals(Set.of("ROLE_CODEFLIX_ADMIN", "ROLE_OUTRO_WRITER"), authorityNames(actual.getAuthorities()));
    }

    @Test
    void givenClaimWithoutTheRolesList_whenConverts_thenThereIsNoAuthority() {
        final var jwt = jwtWith(Map.of(REALM_ACCESS, Map.of("outra-coisa", "sem roles")));

        final var actual = this.converter.convert(jwt);

        assertTrue(actual.getAuthorities().isEmpty());
    }

    @Test
    void givenTokenWithoutRoleClaims_whenConverts_thenThereIsNoAuthority() {
        final var jwt = jwtWith(Map.of());

        final var actual = this.converter.convert(jwt);

        assertTrue(actual.getAuthorities().isEmpty());
    }

    @Test
    void givenAnyToken_whenConverts_thenTheNameIsTheSubject() {
        final var jwt = jwtWith(Map.of(REALM_ACCESS, Map.of(ROLES, List.of("CODEFLIX_ADMIN"))));

        final var actual = this.converter.convert(jwt);

        assertEquals(SUBJECT, actual.getName());
    }

    private static Jwt jwtWith(final Map<String, Object> claims) {
        final var builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(300))
                .subject(SUBJECT);
        claims.forEach(builder::claim);
        return builder.build();
    }

    private static Set<String> authorityNames(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
    }
}
