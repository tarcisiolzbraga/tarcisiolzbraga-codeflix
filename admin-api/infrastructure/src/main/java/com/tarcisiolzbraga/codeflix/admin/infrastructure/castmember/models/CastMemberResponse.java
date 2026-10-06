package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.CastMemberOutput;
import java.time.Instant;

public record CastMemberResponse(
        String id, String name, String type, boolean active, Instant createdAt, Instant updatedAt) {

    public static CastMemberResponse from(final CastMemberOutput output) {
        return new CastMemberResponse(
                output.id(), output.name(), output.type(), output.isActive(), output.createdAt(), output.updatedAt());
    }
}
