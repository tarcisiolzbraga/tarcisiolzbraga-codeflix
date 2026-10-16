package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryCommand;
import java.time.Instant;

// Entrada da mutation de exemplo. Existe para acompanhar o módulo do curso e para semear uma
// categoria à mão sem levantar Kafka e Debezium — não é o caminho normal do dado, que chega pelo
// CDC. Veja o comentário no category.graphqls.
public record GqlCategoryInput(
        String id, String name, String description, boolean active, Instant createdAt, Instant updatedAt) {

    public SaveCategoryCommand toCommand() {
        return new SaveCategoryCommand(
                this.id, this.name, this.description, this.active, this.createdAt, this.updatedAt);
    }
}
