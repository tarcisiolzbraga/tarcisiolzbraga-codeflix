package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

// A ativação não passa por aqui: ela tem rotas próprias.
public record UpdateCategoryRequest(String name, String description) {
}
