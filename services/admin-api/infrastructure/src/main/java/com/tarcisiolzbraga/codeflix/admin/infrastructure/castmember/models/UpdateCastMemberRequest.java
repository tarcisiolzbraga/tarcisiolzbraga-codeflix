package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import io.swagger.v3.oas.annotations.media.Schema;

// Sem o campo active: ativar e desativar têm rotas próprias.
public record UpdateCastMemberRequest(
        @Schema(description = "Nome do membro de elenco") String name,
        @Schema(description = "Papel: ACTOR ou DIRECTOR") String type) {
}
