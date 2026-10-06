package com.tarcisiolzbraga.codeflix.admin.application.castmember.list;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import java.time.Instant;

public record CastMemberListOutput(String id, String name, String type, boolean isActive, Instant createdAt) {

    public static CastMemberListOutput from(final CastMember castMember) {
        return new CastMemberListOutput(
                castMember.getId().getValue(),
                castMember.getName(),
                castMember.getType().name(),
                castMember.isActive(),
                castMember.getCreatedAt());
    }
}
