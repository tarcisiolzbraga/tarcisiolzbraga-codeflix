package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

public record UpdateCategoryRequest(String name, String description, Boolean active) {

    public boolean isActive() {
        return this.active == null || this.active;
    }
}
