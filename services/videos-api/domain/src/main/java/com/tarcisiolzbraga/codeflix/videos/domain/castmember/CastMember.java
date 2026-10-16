package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import com.tarcisiolzbraga.codeflix.videos.domain.Entity;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import java.time.Instant;

// Réplica do membro de elenco que o admin-codeflix governa, com a mesma forma da Category: imutável,
// só with(...), sem newX(...) nem activate()/deactivate().
//
// Leva active, ao contrário do CastMember do catálogo do curso, que não tem o campo: a tabela
// cast_member do admin tem, e sem ele o catálogo não saberia esconder um membro desativado.
public final class CastMember extends Entity<CastMemberID> {

    private final String name;
    private final CastMemberType type;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CastMember(
            final CastMemberID id,
            final String name,
            final CastMemberType type,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id);
        this.name = name;
        this.type = type;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CastMember with(
            final CastMemberID id,
            final String name,
            final CastMemberType type,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        return new CastMember(id, name, type, active, createdAt, updatedAt);
    }

    public static CastMember with(final CastMember castMember) {
        return with(
                castMember.getId(),
                castMember.getName(),
                castMember.getType(),
                castMember.isActive(),
                castMember.getCreatedAt(),
                castMember.getUpdatedAt());
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new CastMemberValidator(this, handler).validate();
    }

    public String getName() {
        return this.name;
    }

    public CastMemberType getType() {
        return this.type;
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
