package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCategoryResponse(@Schema(description = "Identificador da categoria criada") String id) {

    public static CreateCategoryResponse from(final CreateCategoryOutput output) {
        return new CreateCategoryResponse(output.id());
    }
}
