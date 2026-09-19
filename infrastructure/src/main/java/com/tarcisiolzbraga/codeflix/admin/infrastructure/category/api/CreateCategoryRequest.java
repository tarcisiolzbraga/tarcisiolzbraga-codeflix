package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

public record CreateCategoryRequest(String name, String description, Boolean active) {

    // Ausente no JSON significa ativa; o domínio é quem valida o resto.
    public boolean isActive() {
        return this.active == null || this.active;
    }
}
