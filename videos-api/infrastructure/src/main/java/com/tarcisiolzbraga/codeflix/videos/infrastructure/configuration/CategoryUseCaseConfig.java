package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.application.category.delete.DeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.delete.DefaultDeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.get.DefaultGetCategoriesByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.get.GetCategoriesByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.list.DefaultListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.DefaultSaveCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Os casos de uso entram como beans aqui, um @Bean cada: a application não conhece Spring, então
// nenhuma classe dela é anotada.
@Configuration
public class CategoryUseCaseConfig {

    private final CategoryGateway categoryGateway;

    public CategoryUseCaseConfig(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Bean
    SaveCategoryUseCase saveCategoryUseCase() {
        return new DefaultSaveCategoryUseCase(this.categoryGateway);
    }

    @Bean
    DeleteCategoryUseCase deleteCategoryUseCase() {
        return new DefaultDeleteCategoryUseCase(this.categoryGateway);
    }

    @Bean
    ListCategoriesUseCase listCategoriesUseCase() {
        return new DefaultListCategoriesUseCase(this.categoryGateway);
    }

    @Bean
    GetCategoriesByIdUseCase getCategoriesByIdUseCase() {
        return new DefaultGetCategoriesByIdUseCase(this.categoryGateway);
    }
}
