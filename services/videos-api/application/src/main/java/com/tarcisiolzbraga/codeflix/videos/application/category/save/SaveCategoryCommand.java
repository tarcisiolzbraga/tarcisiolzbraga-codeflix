package com.tarcisiolzbraga.codeflix.videos.application.category.save;

import java.time.Instant;

// Os campos chegam crus, como vieram da mensagem do admin-codeflix: montar e validar a Category é
// trabalho do caso de uso, para a entidade de domínio não circular fora desta camada.
public record SaveCategoryCommand(
        String id, String name, String description, boolean active, Instant createdAt, Instant updatedAt) {
}
