package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;

public class Category extends AggregateRoot<CategoryID> {

    private String name;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Category(
            final CategoryID id,
            final String name,
            final String description,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant deletedAt) {
        super(id);
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Category newCategory(final String name, final String description, final boolean isActive) {
        final var now = Instant.now();
        final var deletedAt = isActive ? null : now;
        return new Category(CategoryID.unique(), name, description, isActive, now, now, deletedAt);
    }

    public static Category with(
            final CategoryID id,
            final String name,
            final String description,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant deletedAt) {
        return new Category(id, name, description, active, createdAt, updatedAt, deletedAt);
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new CategoryValidator(this, handler).validate();
    }

    public Category activate() {
        this.deletedAt = null;
        this.active = true;
        this.updatedAt = Instant.now();
        return this;
    }

    public Category deactivate() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
        this.active = false;
        this.updatedAt = Instant.now();
        return this;
    }

    public Category update(final String name, final String description, final boolean isActive) {
        if (isActive) {
            activate();
        } else {
            deactivate();
        }
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isActive() {
        return this.active;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }
}
