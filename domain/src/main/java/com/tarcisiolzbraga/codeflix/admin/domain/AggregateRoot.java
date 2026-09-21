package com.tarcisiolzbraga.codeflix.admin.domain;

import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import java.time.Instant;
import java.util.Objects;

// O que todo agregado tem, como a BaseJpaEntity faz do lado da persistência.
public abstract class AggregateRoot<ID extends Identifier> extends Entity<ID> {

    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    protected AggregateRoot(
            final ID id, final boolean active, final Instant createdAt, final Instant updatedAt) {
        super(id);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "'createdAt' should not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "'updatedAt' should not be null");
    }

    public final void activate() {
        this.active = true;
        refreshUpdatedAt();
    }

    public final void deactivate() {
        this.active = false;
        refreshUpdatedAt();
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

    // Todo método de intenção do agregado que muda dados chama isto no fim.
    protected final void refreshUpdatedAt() {
        this.updatedAt = InstantUtils.now();
    }
}
