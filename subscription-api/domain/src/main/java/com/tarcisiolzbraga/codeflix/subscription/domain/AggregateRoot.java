package com.tarcisiolzbraga.codeflix.subscription.domain;

import java.util.ArrayList;
import java.util.List;

// O que todo agregado daqui tem: o id, a versão do lock otimista e os eventos que ainda não saíram.
//
// Não traz `active`, `createdAt` nem `updatedAt`, ao contrário da admin-api: lá todo agregado tem os
// quatro campos, aqui cada um tem o seu conjunto — a Account não tem data alguma, o Plan tem
// deletedAt e a Subscription tem dueDate e lastRenewDate. Subir isso para a base obrigaria metade
// dos agregados a carregar campo que o negócio deles não tem.
//
// A `version` fica aqui porque é de todos: sem contexto de persistência, o lock otimista é a
// cláusula `WHERE version = :version` que o repositório escreve, e é esta a versão que ela compara.
public abstract class AggregateRoot<ID extends Identifier> extends Entity<ID> {

    private final int version;

    // transient porque é mecanismo de entrega, não dado do agregado, e a exceção que o carregasse
    // não seria serializável.
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(final ID id, final int version) {
        super(id);
        this.version = version;
    }

    public final int version() {
        return this.version;
    }

    // Só o próprio agregado registra: o evento é consequência de uma regra dele, não algo que
    // alguém de fora pendura.
    protected final void registerEvent(final DomainEvent event) {
        this.domainEvents.add(this.assertArgumentNotNull(event, "'event' should not be null"));
    }

    public final List<DomainEvent> domainEvents() {
        return List.copyOf(this.domainEvents);
    }

    // Entregar é esquecer: o mesmo evento não sai duas vezes se o agregado for salvo de novo.
    public final void publishDomainEvents(final DomainEventPublisher publisher) {
        this.assertArgumentNotNull(publisher, "'publisher' should not be null");
        this.domainEvents.forEach(publisher::publishEvent);
        this.domainEvents.clear();
    }
}
