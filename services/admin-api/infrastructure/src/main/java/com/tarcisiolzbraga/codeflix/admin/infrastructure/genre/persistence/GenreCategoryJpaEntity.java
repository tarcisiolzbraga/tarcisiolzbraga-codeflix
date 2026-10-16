package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.envers.Audited;

// Vínculo entre gênero e categoria mapeado como entidade, e não @ElementCollection, para ter
// controle da tabela caso ela ganhe colunas próprias.
@Audited
@Entity(name = "GenreCategory")
@Table(name = "genre_category")
public class GenreCategoryJpaEntity {

    @EmbeddedId
    private GenreCategoryID id;

    @ManyToOne
    @MapsId("genreId")
    private GenreJpaEntity genre;

    protected GenreCategoryJpaEntity() {
    }

    private GenreCategoryJpaEntity(final GenreJpaEntity genre, final CategoryID categoryId) {
        this.id = GenreCategoryID.from(genre.getId(), categoryId.value());
        this.genre = genre;
    }

    public static GenreCategoryJpaEntity from(final GenreJpaEntity genre, final CategoryID categoryId) {
        return new GenreCategoryJpaEntity(genre, categoryId);
    }

    public GenreCategoryID getId() {
        return this.id;
    }

    public GenreJpaEntity getGenre() {
        return this.genre;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(this.id, ((GenreCategoryJpaEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
