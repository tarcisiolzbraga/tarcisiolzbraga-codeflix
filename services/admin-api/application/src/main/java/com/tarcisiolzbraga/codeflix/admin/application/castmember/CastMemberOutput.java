package com.tarcisiolzbraga.codeflix.admin.application.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import java.time.Instant;

public record CastMemberOutput(
        String id, String name, String type, boolean isActive, Instant createdAt, Instant updatedAt) {

    public static CastMemberOutput from(final CastMember castMember) {
        return new CastMemberOutput(
                castMember.getId().getValue(),
                castMember.getName(),
                castMember.getType().name(),
                castMember.isActive(),
                castMember.getCreatedAt(),
                castMember.getUpdatedAt());
    }
}
