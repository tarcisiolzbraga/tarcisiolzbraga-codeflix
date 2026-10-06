package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.persistence.BaseJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.envers.Audited;

@Audited
@Entity(name = "Genre")
@Table(name = "genre")
public class GenreJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false)
    private String name;

    // EAGER porque o agregado sempre é reconstruído inteiro; na listagem, o batch fetch do
    // Hibernate carrega as coleções em lotes, sem N+1 e sem paginar em memória.
    @OneToMany(mappedBy = "genre", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<GenreCategoryJpaEntity> categories = new HashSet<>();

    protected GenreJpaEntity() {
    }

    private GenreJpaEntity(final Genre genre) {
        super(genre.getId().getValue(), genre.isActive(), genre.getCreatedAt(), genre.getUpdatedAt());
        this.name = genre.getName();
        genre.getCategories().forEach(categoryId -> this.categories.add(GenreCategoryJpaEntity.from(this, categoryId)));
    }

    public static GenreJpaEntity from(final Genre genre) {
        return new GenreJpaEntity(genre);
    }

    public Genre toAggregate() {
        return Genre.with(
                GenreID.from(getId()),
                this.name,
                isActive(),
                getCategoryIds(),
                getCreatedAt(),
                getUpdatedAt());
    }

    public String getName() {
        return this.name;
    }

    public Set<CategoryID> getCategoryIds() {
        return this.categories.stream()
                .map(category -> CategoryID.from(category.getId().getCategoryId()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
