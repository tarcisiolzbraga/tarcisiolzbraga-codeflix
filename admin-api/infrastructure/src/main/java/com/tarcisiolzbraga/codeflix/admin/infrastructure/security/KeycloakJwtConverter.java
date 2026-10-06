package com.tarcisiolzbraga.codeflix.admin.infrastructure.security;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

// O Keycloak não põe as roles onde o Spring as procura: elas vêm em realm_access.roles e, quando
// são do client, em resource_access.<client>.roles. Este conversor traduz as duas formas para
// autoridades com o prefixo ROLE_, que é o que hasAnyRole espera.
//
// Diferente do curso, lê as claims como Map comum: o JSONObject sombreado do Nimbus que ele importa
// não existe mais nas versões atuais.
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String CLIENT_ROLE_SEPARATOR = "_";

    @Override
    public AbstractAuthenticationToken convert(final Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authoritiesOf(jwt), jwt.getClaimAsString(JwtClaimNames.SUB));
    }

    private Collection<GrantedAuthority> authoritiesOf(final Jwt jwt) {
        return Stream.concat(realmRolesOf(jwt), clientRolesOf(jwt))
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Stream<String> realmRolesOf(final Jwt jwt) {
        return rolesIn(jwt.getClaimAsMap(REALM_ACCESS)).stream();
    }

    // A role do client é qualificada pelo nome dele, senão duas roles homônimas de clients
    // diferentes virariam a mesma autoridade.
    private Stream<String> clientRolesOf(final Jwt jwt) {
        return Optional.ofNullable(jwt.getClaimAsMap(RESOURCE_ACCESS))
                .orElse(Map.of())
                .entrySet()
                .stream()
                .flatMap(this::qualifiedRolesOf);
    }

    private Stream<String> qualifiedRolesOf(final Map.Entry<String, Object> client) {
        return rolesIn(asMap(client.getValue())).stream()
                .map(role -> client.getKey() + CLIENT_ROLE_SEPARATOR + role);
    }

    private Collection<String> rolesIn(final Map<String, Object> claim) {
        if (claim == null || !(claim.get(ROLES) instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream().map(String::valueOf).toList();
    }

    private Map<String, Object> asMap(final Object value) {
        return value instanceof Map<?, ?> map
                ? map.entrySet().stream()
                        .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue))
                : Map.of();
    }
}
