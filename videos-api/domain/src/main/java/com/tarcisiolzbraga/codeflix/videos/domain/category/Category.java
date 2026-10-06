package com.tarcisiolzbraga.codeflix.videos.domain.category;

import com.tarcisiolzbraga.codeflix.videos.domain.Entity;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import java.time.Instant;

// Réplica da categoria que o admin-codeflix governa. Por isso é imutável e só tem with(...): não
// existe newCategory(), porque este lado não cria categoria, nem activate()/deactivate(), porque
// mudar o estado aqui seria mentir sobre quem manda no dado — a mudança chega como um dado novo.
// Também não há deletedAt: no admin-codeflix o delete é físico, então não existe esse estado para
// replicar.
public final class Category extends Entity<CategoryID> {

    private final String name;
    private final String description;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public static Category with(final Category category) {
        return with(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new CategoryValidator(this, handler).validate();
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
