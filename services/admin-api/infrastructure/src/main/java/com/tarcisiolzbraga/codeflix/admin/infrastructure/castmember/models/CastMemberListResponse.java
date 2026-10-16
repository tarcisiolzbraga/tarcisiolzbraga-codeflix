package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.CastMemberListOutput;
import java.time.Instant;

public record CastMemberListResponse(String id, String name, String type, boolean active, Instant createdAt) {

    public static CastMemberListResponse from(final CastMemberListOutput output) {
        return new CastMemberListResponse(
                output.id(), output.name(), output.type(), output.isActive(), output.createdAt());
    }
}
