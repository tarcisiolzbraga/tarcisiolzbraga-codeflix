package com.tarcisiolzbraga.codeflix.subscription.domain;

public abstract class Entity<ID extends Identifier> implements AssertionConcern {

    protected final ID id;

    protected Entity(final ID id) {
        this.id = this.assertArgumentNotNull(id, "'id' should not be null");
    }

    public ID id() {
        return this.id;
    }

    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Entity<?> that)) {
            return false;
        }
        return this.id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        return this.id.hashCode();
    }
}
