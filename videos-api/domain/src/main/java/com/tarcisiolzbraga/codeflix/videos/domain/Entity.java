package com.tarcisiolzbraga.codeflix.videos.domain;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import java.util.Objects;

// Não há AggregateRoot aqui: este catálogo é o lado de leitura e não governa nenhum dado. As
// entidades são réplicas imutáveis do que o admin-codeflix publica, então não existe estado comum
// para mudar nem evento para registrar — só identidade e validação.
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
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Entity<?> that)) {
            return false;
        }
        return Objects.equals(this.id, that.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.id);
    }
}
