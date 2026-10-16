package com.tarcisiolzbraga.codeflix.admin.application.castmember.update;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;

public record UpdateCastMemberOutput(String id) {

    public static UpdateCastMemberOutput from(final CastMember castMember) {
        return new UpdateCastMemberOutput(castMember.getId().getValue());
    }
}
