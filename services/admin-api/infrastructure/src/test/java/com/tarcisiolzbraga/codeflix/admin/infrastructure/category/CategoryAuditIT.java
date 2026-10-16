package com.tarcisiolzbraga.codeflix.admin.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryJpaEntity;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CategoryAuditIT {

    @Autowired
    private CategoryMySQLGateway categoryGateway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void givenCreatedCategory_whenReadRevisions_thenHaveOneRevision() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", "A mais assistida", true));

        final var revisions = revisionsOf(category.getId().value());

        assertEquals(1, revisions.size());
        assertEquals("Filmes", nameAt(category.getId().value(), revisions.getFirst()));
    }

    @Test
    void givenUpdatedCategory_whenReadRevisions_thenKeepThePreviousName() {
        final var category = this.categoryGateway.create(Category.newCategory("Flmes", null, true));
        this.categoryGateway.update(category.update("Filmes", "A mais assistida"));

        final var revisions = revisionsOf(category.getId().value());

        assertEquals(2, revisions.size());
        assertEquals("Flmes", nameAt(category.getId().value(), revisions.getFirst()));
        assertEquals("Filmes", nameAt(category.getId().value(), revisions.get(1)));
    }

    @Test
    void givenDeletedCategory_whenReadRevisions_thenRecordTheDeletion() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true));
        this.categoryGateway.deleteById(category.getId());

        final var revisions = revisionsOf(category.getId().value());

        assertEquals(2, revisions.size());
        assertNull(nameAt(category.getId().value(), revisions.get(1)));
        assertNotNull(nameAt(category.getId().value(), revisions.getFirst()));
    }

    @Test
    void givenDeactivatedCategory_whenReadLastRevision_thenAuditTheInheritedFields() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true));
        category.deactivate();
        this.categoryGateway.update(category);
        final var revisions = revisionsOf(category.getId().value());

        final var audited = auditedAt(category.getId().value(), revisions.getLast());

        assertEquals(category.getId().value(), audited.getId());
        assertNotNull(audited.getCreatedAt());
        assertNotNull(audited.getUpdatedAt());
        assertFalse(audited.isActive());
        assertTrue(auditedAt(category.getId().value(), revisions.getFirst()).isActive());
    }

    private List<Number> revisionsOf(final UUID id) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).getRevisions(CategoryJpaEntity.class, id);
        }
    }

    private String nameAt(final UUID id, final Number revision) {
        final var audited = auditedAt(id, revision);
        return audited == null ? null : audited.getName();
    }

    private CategoryJpaEntity auditedAt(final UUID id, final Number revision) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).find(CategoryJpaEntity.class, id, revision);
        }
    }
}
