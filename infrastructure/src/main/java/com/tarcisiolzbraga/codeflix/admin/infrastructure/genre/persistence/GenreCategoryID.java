package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class GenreCategoryID implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "genre_id", length = 36, nullable = false)
    private String genreId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", length = 36, nullable = false)
    private String categoryId;

    protected GenreCategoryID() {
    }

    private GenreCategoryID(final String genreId, final String categoryId) {
        this.genreId = genreId;
        this.categoryId = categoryId;
    }

    public static GenreCategoryID from(final String genreId, final String categoryId) {
        return new GenreCategoryID(genreId, categoryId);
    }

    public String getGenreId() {
        return this.genreId;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final var that = (GenreCategoryID) other;
        return Objects.equals(this.genreId, that.genreId) && Objects.equals(this.categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.genreId, this.categoryId);
    }
}
