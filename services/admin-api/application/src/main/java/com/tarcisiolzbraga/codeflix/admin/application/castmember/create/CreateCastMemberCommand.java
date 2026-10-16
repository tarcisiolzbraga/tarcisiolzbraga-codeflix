package com.tarcisiolzbraga.codeflix.admin.application.castmember.create;

// O tipo chega como texto, e não como enum: valor desconhecido é erro de validação do caso de uso,
// não falha de desserialização, e o enum do domínio não vaza para o contrato da API.
public record CreateCastMemberCommand(String name, String type, boolean isActive) {

    public static CreateCastMemberCommand with(final String name, final String type, final boolean isActive) {
        return new CreateCastMemberCommand(name, type, isActive);
    }
}
