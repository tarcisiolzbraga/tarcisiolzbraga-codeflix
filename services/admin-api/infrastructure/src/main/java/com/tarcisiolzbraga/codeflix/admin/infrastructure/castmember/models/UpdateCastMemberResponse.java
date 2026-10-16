package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateCastMemberResponse(
        @Schema(description = "Identificador do membro de elenco atualizado") String id) {

    public static UpdateCastMemberResponse from(final UpdateCastMemberOutput output) {
        return new UpdateCastMemberResponse(output.id());
    }
}
