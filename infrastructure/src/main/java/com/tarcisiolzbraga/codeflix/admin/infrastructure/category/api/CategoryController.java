package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryCommand;
import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryOutput;
import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.delete.DeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.get.GetCategoryByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.update.UpdateCategoryCommand;
import com.tarcisiolzbraga.codeflix.admin.application.category.update.UpdateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private static final String RESOURCE_PATH = "/categories/";

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryByIdUseCase getCategoryByIdUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;

    public CategoryController(
            final CreateCategoryUseCase createCategoryUseCase,
            final GetCategoryByIdUseCase getCategoryByIdUseCase,
            final UpdateCategoryUseCase updateCategoryUseCase,
            final DeleteCategoryUseCase deleteCategoryUseCase,
            final ListCategoriesUseCase listCategoriesUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.getCategoryByIdUseCase = getCategoryByIdUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody final CreateCategoryRequest request) {
        final var command = CreateCategoryCommand.with(request.name(), request.description(), request.isActive());
        return this.createCategoryUseCase.execute(command).fold(this::unprocessableContent, this::created);
    }

    @GetMapping
    public Pagination<CategoryListResponse> list(@ModelAttribute final CategorySearchRequest request) {
        return this.listCategoriesUseCase.execute(request.toSearchQuery()).map(CategoryListResponse::from);
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable("id") final String id) {
        return CategoryResponse.from(this.getCategoryByIdUseCase.execute(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(
            @PathVariable("id") final String id, @RequestBody final UpdateCategoryRequest request) {
        final var command =
                UpdateCategoryCommand.with(id, request.name(), request.description(), request.isActive());
        return this.updateCategoryUseCase.execute(command).fold(this::unprocessableContent, ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable("id") final String id) {
        this.deleteCategoryUseCase.execute(id);
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateCategoryOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id())).body(output);
    }
}
