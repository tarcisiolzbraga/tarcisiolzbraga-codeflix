package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;

public class Category extends AggregateRoot<CategoryID> {

    private String name;
    private String description;

    private Category(
            final CategoryID id,
            final String name,
            final String description,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id, active, createdAt, updatedAt);
        this.name = name;
        this.description = description;
    }

    public static Category newCategory(final String name, final String description, final boolean isActive) {
        final var now = InstantUtils.now();
        return new Category(CategoryID.unique(), name, description, isActive, now, now);
    }

    public static Category with(
            final CategoryID id,
            final String name,
            final String description,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Category(id, name, description, active, createdAt, updatedAt);
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new CategoryValidator(this, handler).validate();
    }

    public Category update(final String name, final String description) {
        this.name = name;
        this.description = description;
        refreshUpdatedAt();
        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }
}
