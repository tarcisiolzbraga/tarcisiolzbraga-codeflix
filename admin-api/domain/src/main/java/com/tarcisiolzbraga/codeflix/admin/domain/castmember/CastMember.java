package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;

public class CastMember extends AggregateRoot<CastMemberID> {

    private String name;
    private CastMemberType type;

    private CastMember(
            final CastMemberID id,
            final String name,
            final CastMemberType type,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id, active, createdAt, updatedAt);
        this.name = name;
        this.type = type;
    }

    public static CastMember newCastMember(final String name, final CastMemberType type, final boolean isActive) {
        final var now = InstantUtils.now();
        return new CastMember(CastMemberID.unique(), name, type, isActive, now, now);
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

    @Override
    public void validate(final ValidationHandler handler) {
        new CastMemberValidator(this, handler).validate();
    }

    public CastMember update(final String name, final CastMemberType type) {
        this.name = name;
        this.type = type;
        refreshUpdatedAt();
        return this;
    }

    public String getName() {
        return this.name;
    }

    public CastMemberType getType() {
        return this.type;
    }
}
