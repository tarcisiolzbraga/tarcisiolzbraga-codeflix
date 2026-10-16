package com.tarcisiolzbraga.codeflix.admin.application.castmember.update;

// Sem isActive: ativar e desativar têm caso de uso próprio.
public record UpdateCastMemberCommand(String id, String name, String type) {

    public static UpdateCastMemberCommand with(final String id, final String name, final String type) {
        return new UpdateCastMemberCommand(id, name, type);
    }
}
