package com.tarcisiolzbraga.codeflix.videos.application.genre.save;

import java.time.Instant;
import java.util.Set;

// Os campos chegam crus, como vieram da mensagem do admin-codeflix: as categorias como texto, que é
// o caso de uso que converte em id tipado.
public record SaveGenreCommand(
        String id, String name, boolean active, Set<String> categories, Instant createdAt, Instant updatedAt) {
}
