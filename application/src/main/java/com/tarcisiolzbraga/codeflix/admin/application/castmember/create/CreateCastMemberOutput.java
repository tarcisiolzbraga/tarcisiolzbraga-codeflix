package com.tarcisiolzbraga.codeflix.admin.application.castmember.create;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;

public record CreateCastMemberOutput(String id) {

    public static CreateCastMemberOutput from(final CastMember castMember) {
        return new CreateCastMemberOutput(castMember.getId().getValue());
    }
}
