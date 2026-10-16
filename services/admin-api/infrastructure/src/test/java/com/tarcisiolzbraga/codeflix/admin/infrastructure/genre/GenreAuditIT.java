package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.CategoryMySQLGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreJpaEntity;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class GenreAuditIT {

    @Autowired
    private GenreMySQLGateway genreGateway;

    @Autowired
    private CategoryMySQLGateway categoryGateway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void givenGenreWithCategory_whenReadFirstRevision_thenHaveTheCategory() {
        final var movies = existingCategory("Filmes");
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", true).addCategory(movies));

        final var revisions = revisionsOf(genre.getId().value());

        assertEquals(1, revisions.size());
        assertEquals(Set.of(movies), categoriesAt(genre.getId().value(), revisions.getFirst()));
    }

    @Test
    void givenCategoryRemovedFromGenre_whenReadRevisions_thenKeepTheLinkInThePreviousOne() {
        final var movies = existingCategory("Filmes");
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", true).addCategory(movies));
        this.genreGateway.update(genre.removeCategory(movies));

        final var revisions = revisionsOf(genre.getId().value());

        assertEquals(2, revisions.size());
        assertEquals(Set.of(movies), categoriesAt(genre.getId().value(), revisions.getFirst()));
        assertEquals(Set.of(), categoriesAt(genre.getId().value(), revisions.get(1)));
    }

    @Test
    void givenDeletedGenre_whenReadRevisions_thenRecordTheDeletion() {
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", true).addCategory(existingCategory("Filmes")));
        this.genreGateway.deleteById(genre.getId());

        final var revisions = revisionsOf(genre.getId().value());

        assertEquals(2, revisions.size());
        assertNull(auditedAt(genre.getId().value(), revisions.get(1)));
    }

    private CategoryID existingCategory(final String name) {
        return this.categoryGateway.create(Category.newCategory(name, null, true)).getId();
    }

    private List<Number> revisionsOf(final UUID id) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).getRevisions(GenreJpaEntity.class, id);
        }
    }

    // Lê as categorias com o EntityManager aberto: o Envers carrega a coleção auditada sob demanda.
    private Set<CategoryID> categoriesAt(final UUID id, final Number revision) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).find(GenreJpaEntity.class, id, revision).getCategoryIds();
        }
    }

    private GenreJpaEntity auditedAt(final UUID id, final Number revision) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).find(GenreJpaEntity.class, id, revision);
        }
    }
}
