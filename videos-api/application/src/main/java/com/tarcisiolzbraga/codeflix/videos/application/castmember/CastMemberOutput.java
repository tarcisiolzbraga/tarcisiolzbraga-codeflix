package com.tarcisiolzbraga.codeflix.videos.application.castmember;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import java.time.Instant;

// Um único output serve às duas leituras, como no CategoryOutput. O tipo sai como enum, e não como
// texto: ele é value object do domínio, não entidade, e assim a borda pode expor um enum de verdade
// no schema em vez de uma string que aceita qualquer coisa.
public record CastMemberOutput(
        String id, String name, CastMemberType type, boolean active, Instant createdAt, Instant updatedAt) {

    public static CastMemberOutput from(final CastMember castMember) {
        return new CastMemberOutput(
                castMember.getId().getValue(),
                castMember.getName(),
                castMember.getType(),
                castMember.isActive(),
                castMember.getCreatedAt(),
                castMember.getUpdatedAt());
    }
}
