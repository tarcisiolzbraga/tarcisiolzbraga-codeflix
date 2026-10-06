package com.tarcisiolzbraga.codeflix.videos.application.castmember.save;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;

public record SaveCastMemberOutput(String id) {

    public static SaveCastMemberOutput from(final CastMember castMember) {
        return new SaveCastMemberOutput(castMember.getId().getValue());
    }
}
