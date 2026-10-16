package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCastMemberRequest(
        @Schema(description = "Nome do membro de elenco") String name,
        @Schema(description = "Papel: ACTOR ou DIRECTOR") String type,
        @Schema(description = "Ausente significa ativo") Boolean active) {

    // Ausente no JSON significa ativo; o domínio é quem valida o resto.
    public boolean isActive() {
        return this.active == null || this.active;
    }
}
