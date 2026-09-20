package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;
import java.util.Objects;

public class Category extends AggregateRoot<CategoryID> {

    private String name;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Category(
            final CategoryID id,
            final String name,
            final String description,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id);
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "'createdAt' should not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "'updatedAt' should not be null");
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

    public Category activate() {
        this.active = true;
        this.updatedAt = InstantUtils.now();
        return this;
    }

    public Category deactivate() {
        this.active = false;
        this.updatedAt = InstantUtils.now();
        return this;
    }

    // Não mexe na ativação: isso é responsabilidade do activate() e do deactivate().
    public Category update(final String name, final String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = InstantUtils.now();
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
}
