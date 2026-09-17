package com.tarcisiolzbraga.codeflix.admin.domain;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.Objects;

public abstract class Entity<ID extends Identifier> {

    protected final ID id;

    protected Entity(final ID id) {
        this.id = Objects.requireNonNull(id, "'id' should not be null");
    }

    public abstract void validate(ValidationHandler handler);

    public ID getId() {
        return this.id;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Entity<?> that)) {
            return false;
        }
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
