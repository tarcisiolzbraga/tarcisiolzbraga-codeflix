package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.create.DefaultCreateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.delete.DefaultDeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.delete.DeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.get.DefaultGetCategoryByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.get.GetCategoryByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.list.DefaultListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.update.DefaultUpdateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.update.UpdateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O component scan só enxerga a infrastructure, então os casos de uso são registrados aqui.
@Configuration
public class CategoryUseCaseConfig {

    private final CategoryGateway categoryGateway;

    public CategoryUseCaseConfig(final CategoryGateway categoryGateway) {
        this.categoryGateway = categoryGateway;
    }

    @Bean
    CreateCategoryUseCase createCategoryUseCase() {
        return new DefaultCreateCategoryUseCase(this.categoryGateway);
    }

    @Bean
    GetCategoryByIdUseCase getCategoryByIdUseCase() {
        return new DefaultGetCategoryByIdUseCase(this.categoryGateway);
    }

    @Bean
    UpdateCategoryUseCase updateCategoryUseCase() {
        return new DefaultUpdateCategoryUseCase(this.categoryGateway);
    }

    @Bean
    DeleteCategoryUseCase deleteCategoryUseCase() {
        return new DefaultDeleteCategoryUseCase(this.categoryGateway);
    }

    @Bean
    ListCategoriesUseCase listCategoriesUseCase() {
        return new DefaultListCategoriesUseCase(this.categoryGateway);
    }
}
