package com.tarcisiolzbraga.codeflix.admin.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CategoryAuditIT {

    @Autowired
    private CategoryMySQLGateway categoryGateway;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void cleanUp() {
        this.categoryRepository.deleteAll();
    }

    @Test
    void givenCreatedCategory_whenReadRevisions_thenHaveOneRevision() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", "A mais assistida", true));

        final var revisions = revisionsOf(category.getId().getValue());

        assertEquals(1, revisions.size());
        assertEquals("Filmes", nameAt(category.getId().getValue(), revisions.getFirst()));
    }

    @Test
    void givenUpdatedCategory_whenReadRevisions_thenKeepThePreviousName() {
        final var category = this.categoryGateway.create(Category.newCategory("Flmes", null, true));
        this.categoryGateway.update(category.update("Filmes", "A mais assistida", true));

        final var revisions = revisionsOf(category.getId().getValue());

        assertEquals(2, revisions.size());
        assertEquals("Flmes", nameAt(category.getId().getValue(), revisions.getFirst()));
        assertEquals("Filmes", nameAt(category.getId().getValue(), revisions.get(1)));
    }

    @Test
    void givenDeletedCategory_whenReadRevisions_thenRecordTheDeletion() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true));
        this.categoryGateway.deleteById(category.getId());

        final var revisions = revisionsOf(category.getId().getValue());

        assertEquals(2, revisions.size());
        assertNull(nameAt(category.getId().getValue(), revisions.get(1)));
        assertNotNull(nameAt(category.getId().getValue(), revisions.getFirst()));
    }

    private List<Number> revisionsOf(final String id) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).getRevisions(CategoryJpaEntity.class, id);
        }
    }

    private String nameAt(final String id, final Number revision) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            final var audited = AuditReaderFactory.get(entityManager)
                    .find(CategoryJpaEntity.class, id, revision);
            return audited == null ? null : audited.getName();
        }
    }
}
