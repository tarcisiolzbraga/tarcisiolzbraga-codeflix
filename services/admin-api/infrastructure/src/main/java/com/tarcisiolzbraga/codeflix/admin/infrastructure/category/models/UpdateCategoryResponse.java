package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.admin.application.category.update.UpdateCategoryOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateCategoryResponse(@Schema(description = "Identificador da categoria atualizada") String id) {

    public static UpdateCategoryResponse from(final UpdateCategoryOutput output) {
        return new UpdateCategoryResponse(output.id());
    }
}
