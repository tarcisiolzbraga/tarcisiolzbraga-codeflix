package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;

// O membro de elenco como o cliente do catálogo o vê. Sem active nem datas, como no GqlCategory:
// eles servem à replicação, não a quem consome.
//
// O tipo sai como enum, então o schema expõe um enum de verdade e o cliente não consegue pedir um
// papel que não existe.
public record GqlCastMember(String id, String name, CastMemberType type) {

    public static GqlCastMember from(final CastMemberOutput output) {
        return new GqlCastMember(output.id(), output.name(), output.type());
    }
}
