package com.tarcisiolzbraga.codeflix.videos.application.castmember.save;

import java.time.Instant;

// Os campos chegam crus, como vieram da mensagem do admin-codeflix — o tipo inclusive, que vem como
// texto e é o caso de uso que o converte.
public record SaveCastMemberCommand(
        String id, String name, String type, boolean active, Instant createdAt, Instant updatedAt) {
}
