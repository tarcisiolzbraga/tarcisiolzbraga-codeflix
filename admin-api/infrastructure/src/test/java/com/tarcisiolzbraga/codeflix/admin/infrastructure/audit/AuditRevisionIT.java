package com.tarcisiolzbraga.codeflix.admin.infrastructure.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.CategoryMySQLGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.security.KeycloakJwtConverter;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

// O autor chegando ao banco. O unitário prova que o listener lê a identidade certa; aqui o assunto
// é o resto do caminho: o Envers chamar o listener na gravação e a coluna da V9 guardar o valor.
//
// A categoria é só o veículo — o autor mora no revinfo, que é um só para todos os agregados.
@IntegrationTest
class AuditRevisionIT {

    private static final String SUBJECT = "8094933a-fcd1-495e-87f7-f9a9062c5533";

    @Autowired
    private CategoryMySQLGateway categoryGateway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAnAuthenticatedRequest_whenWrite_thenRecordTheAuthorInTheRevision() {
        authenticate();

        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true));

        assertEquals(SUBJECT, authorOfLastRevisionOf(category.getId().value()));
    }

    // O retorno do codificador e o relay da saída gravam assim, sem token nenhum.
    @Test
    void givenNoAuthentication_whenWrite_thenLeaveTheAuthorNull() {
        final var category = this.categoryGateway.create(Category.newCategory("Séries", null, true));

        assertNull(authorOfLastRevisionOf(category.getId().value()));
    }

    @Test
    void givenTwoWritesByDifferentAuthors_whenReadRevisions_thenEachKeepsItsOwn() {
        authenticate();
        final var category = this.categoryGateway.create(Category.newCategory("Flmes", null, true));
        SecurityContextHolder.clearContext();

        this.categoryGateway.update(category.update("Filmes", null));

        final var revisions = revisionsOf(category.getId().value());
        assertEquals(SUBJECT, authorOf(revisions.getFirst()));
        assertNull(authorOf(revisions.getLast()));
    }

    private void authenticate() {
        final var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(SUBJECT)
                .claim("realm_access", Map.of("roles", List.of("CODEFLIX_ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new KeycloakJwtConverter().convert(jwt));
        SecurityContextHolder.setContext(context);
    }

    private String authorOfLastRevisionOf(final UUID id) {
        return authorOf(revisionsOf(id).getLast());
    }

    private List<Number> revisionsOf(final UUID id) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).getRevisions(CategoryJpaEntity.class, id);
        }
    }

    private String authorOf(final Number revision) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager)
                    .findRevision(AuditRevision.class, revision)
                    .getUserId();
        }
    }
}
