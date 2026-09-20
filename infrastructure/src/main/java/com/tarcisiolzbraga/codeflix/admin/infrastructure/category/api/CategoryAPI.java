package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.CREATED;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.ID_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NOT_FOUND_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NO_CONTENT;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CategoryListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CategoryResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CategorySearchRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CreateCategoryRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CreateCategoryResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.UpdateCategoryRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.UpdateCategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
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

// Contrato HTTP e documentação OpenAPI das categorias; o CategoryController só implementa.
@Tag(name = "Categorias", description = "Cadastro das categorias do catálogo")
@RequestMapping("/categories")
public interface CategoryAPI {

    @PostMapping
    @Operation(summary = "Cria uma categoria", description = "Sem o campo active, a categoria nasce ativa.")
    @ApiResponse(responseCode = CREATED, description = "Criada; a URL do recurso vem no header Location",
            content = @Content(schema = @Schema(implementation = CreateCategoryResponse.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> create(@RequestBody CreateCategoryRequest request);

    @GetMapping
    @Operation(summary = "Lista as categorias, paginadas", description = "search filtra por nome ou descrição.")
    @ApiResponse(responseCode = OK, description = "Página de categorias")
    Pagination<CategoryListResponse> list(@ParameterObject @ModelAttribute CategorySearchRequest request);

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma categoria pelo id")
    @ApiResponse(responseCode = OK, description = "Categoria encontrada")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryResponse getById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria", description = "Substitui nome e descrição; a ativação tem rotas próprias.")
    @ApiResponse(responseCode = OK, description = "Atualizada",
            content = @Content(schema = @Schema(implementation = UpdateCategoryResponse.class)))
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> update(
            @Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id,
            @RequestBody UpdateCategoryRequest request);

    @PutMapping("/{id}/activate")
    @Operation(summary = "Ativa uma categoria", description = "Idempotente: uma categoria já ativa segue ativa.")
    @ApiResponse(responseCode = OK, description = "Categoria ativa")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryResponse activate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Desativa uma categoria", description = "Tira do catálogo sem apagar o registro.")
    @ApiResponse(responseCode = OK, description = "Categoria inativa")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryResponse deactivate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma categoria", description = "Idempotente: id inexistente também responde 204.")
    @ApiResponse(responseCode = NO_CONTENT, description = "Removida, ou já não existia")
    void deleteById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);
}
