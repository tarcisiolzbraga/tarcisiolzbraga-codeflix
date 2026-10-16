package com.tarcisiolzbraga.codeflix.admin.domain;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// O que todo agregado tem, como a BaseJpaEntity faz do lado da persistência.
public abstract class AggregateRoot<ID extends Identifier> extends Entity<ID> {

    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    // Os eventos ficam aqui, e não na Entity como no curso: quem fala com o mundo de fora é a raiz
    // do agregado. Nascem vazios também na reconstrução vinda do banco, que não é um fato novo.
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

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

    // Só o próprio agregado registra: o evento é consequência de uma regra dele, não algo que
    // alguém de fora pendura.
    protected final void registerEvent(final DomainEvent event) {
        this.domainEvents.add(Objects.requireNonNull(event, "'event' should not be null"));
    }

    public List<DomainEvent> getDomainEvents() {
        return List.copyOf(this.domainEvents);
    }

    // Entregar é esquecer: o mesmo evento não sai duas vezes se o agregado for salvo de novo.
    public final void publishDomainEvents(final DomainEventPublisher publisher) {
        Objects.requireNonNull(publisher, "'publisher' should not be null");
        this.domainEvents.forEach(publisher::publishEvent);
        this.domainEvents.clear();
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
