package com.tarcisiolzbraga.codeflix.admin.e2e;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CategoryListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CategoryResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CreateCategoryRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CreateCategoryResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.UpdateCategoryRequest;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP, sem atalho pelo gateway ou pelo banco.
public interface CategoryE2EDsl {

    String CATEGORIES_PATH = "/categories";

    RestClient client();

    default String givenACategory(final String name, final String description) {
        return createACategory(new CreateCategoryRequest(name, description, null)).id();
    }

    default CreateCategoryResponse createACategory(final CreateCategoryRequest request) {
        return client().post()
                .uri(CATEGORIES_PATH)
                .body(request)
                .retrieve()
                .body(CreateCategoryResponse.class);
    }

    default CategoryResponse retrieveACategory(final String id) {
        return client().get()
                .uri(CATEGORIES_PATH + "/{id}", id)
                .retrieve()
                .body(CategoryResponse.class);
    }

    default Pagination<CategoryListResponse> listCategories(final int page, final int perPage) {
        return listCategories(page, perPage, null);
    }

    default Pagination<CategoryListResponse> listCategories(
            final int page, final int perPage, final String search) {
        return client().get()
                .uri(builder -> builder.path(CATEGORIES_PATH)
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParamIfPresent("search", Optional.ofNullable(search))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    default void updateACategory(final String id, final String name, final String description) {
        client().put()
                .uri(CATEGORIES_PATH + "/{id}", id)
                .body(new UpdateCategoryRequest(name, description))
                .retrieve()
                .toBodilessEntity();
    }

    default CategoryResponse activateACategory(final String id) {
        return changeActivation(id, "activate");
    }

    default CategoryResponse deactivateACategory(final String id) {
        return changeActivation(id, "deactivate");
    }

    default void deleteACategory(final String id) {
        client().delete().uri(CATEGORIES_PATH + "/{id}", id).retrieve().toBodilessEntity();
    }

    private CategoryResponse changeActivation(final String id, final String action) {
        return client().put()
                .uri(CATEGORIES_PATH + "/{id}/{action}", id, action)
                .retrieve()
                .body(CategoryResponse.class);
    }
}
