package com.tarcisiolzbraga.codeflix.admin.infrastructure.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.security.KeycloakJwtConverter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

// O token não é inventado: passa pelo KeycloakJwtConverter de verdade, que é quem decide o que vira
// o nome do Authentication. Assim o teste cobre a emenda entre os dois, e não só o listener.
class AuditRevisionListenerTest {

    private static final String SUBJECT = "8094933a-fcd1-495e-87f7-f9a9062c5533";

    private final AuditRevisionListener listener = new AuditRevisionListener();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAnAuthenticatedRequest_whenNewRevision_thenRecordTheSubject() {
        authenticateWith(new KeycloakJwtConverter().convert(jwt()));
        final var revision = new AuditRevision();

        this.listener.newRevision(revision);

        assertEquals(SUBJECT, revision.getUserId());
    }

    @Test
    void givenNoAuthentication_whenNewRevision_thenLeaveTheUserNull() {
        final var revision = new AuditRevision();

        this.listener.newRevision(revision);

        assertNull(revision.getUserId());
    }

    // O filtro de anônimo do Spring entrega um Authentication que se diz autenticado; sem este
    // caso, toda gravação sem token gravaria "anonymousUser" como se fosse gente.
    @Test
    void givenAnAnonymousAuthentication_whenNewRevision_thenLeaveTheUserNull() {
        authenticateWith(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        final var revision = new AuditRevision();

        this.listener.newRevision(revision);

        assertNull(revision.getUserId());
    }

    private void authenticateWith(final Authentication authentication) {
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(SUBJECT)
                .claim("realm_access", Map.of("roles", List.of("CODEFLIX_ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
