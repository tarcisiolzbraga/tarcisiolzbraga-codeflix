package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCastMemberResponse(
        @Schema(description = "Identificador do membro de elenco criado") String id) {

    public static CreateCastMemberResponse from(final CreateCastMemberOutput output) {
        return new CreateCastMemberResponse(output.id());
    }
}
