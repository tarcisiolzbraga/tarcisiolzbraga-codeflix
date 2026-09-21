package com.tarcisiolzbraga.codeflix.admin.domain.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Genre extends AggregateRoot<GenreID> {

    private static final String CATEGORIES_NOT_NULL_MESSAGE = "'categories' should not be null";
    private static final String CATEGORY_ID_NOT_NULL_MESSAGE = "'categoryID' should not be null";

    private String name;
    // Referência a outro agregado só pelo ID, e em Set porque a mesma categoria não se repete.
    private final Set<CategoryID> categories;

    private Genre(
            final GenreID id,
            final String name,
            final boolean active,
            final Set<CategoryID> categories,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id, active, createdAt, updatedAt);
        this.name = name;
        this.categories = new LinkedHashSet<>(Objects.requireNonNull(categories, CATEGORIES_NOT_NULL_MESSAGE));
    }

    public static Genre newGenre(final String name, final boolean isActive) {
        final var now = InstantUtils.now();
        return new Genre(GenreID.unique(), name, isActive, Set.of(), now, now);
    }

    public static Genre with(
            final GenreID id,
            final String name,
            final boolean active,
            final Set<CategoryID> categories,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Genre(id, name, active, categories, createdAt, updatedAt);
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new GenreValidator(this, handler).validate();
    }

    public Genre update(final String name, final Set<CategoryID> categories) {
        Objects.requireNonNull(categories, CATEGORIES_NOT_NULL_MESSAGE);
        this.name = name;
        this.categories.clear();
        this.categories.addAll(categories);
        refreshUpdatedAt();
        return this;
    }

    public Genre addCategory(final CategoryID categoryID) {
        Objects.requireNonNull(categoryID, CATEGORY_ID_NOT_NULL_MESSAGE);
        this.categories.add(categoryID);
        refreshUpdatedAt();
        return this;
    }

    public Genre addCategories(final Set<CategoryID> categories) {
        Objects.requireNonNull(categories, CATEGORIES_NOT_NULL_MESSAGE);
        if (categories.isEmpty()) {
            return this;
        }
        this.categories.addAll(categories);
        refreshUpdatedAt();
        return this;
    }

    public Genre removeCategory(final CategoryID categoryID) {
        Objects.requireNonNull(categoryID, CATEGORY_ID_NOT_NULL_MESSAGE);
        this.categories.remove(categoryID);
        refreshUpdatedAt();
        return this;
    }

    public String getName() {
        return this.name;
    }

    public Set<CategoryID> getCategories() {
        return Set.copyOf(this.categories);
    }
}
