package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.CREATED;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.ID_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NOT_FOUND_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreSearchRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.UpdateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.UpdateGenreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Contrato HTTP e documentação OpenAPI dos gêneros; o GenreController só implementa.
@Tag(name = "Gêneros", description = "Cadastro dos gêneros do catálogo")
@RequestMapping("/genres")
public interface GenreAPI {

    @PostMapping
    @Operation(
            summary = "Cria um gênero",
            description = "Sem o campo active, o gênero nasce ativo. As categorias informadas precisam existir.")
    @ApiResponse(responseCode = CREATED, description = "Criado; a URL do recurso vem no header Location",
            content = @Content(schema = @Schema(implementation = CreateGenreResponse.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> create(@RequestBody CreateGenreRequest request);

    @GetMapping
    @Operation(summary = "Lista os gêneros, paginados", description = "search filtra pelo nome.")
    @ApiResponse(responseCode = OK, description = "Página de gêneros")
    Pagination<GenreListResponse> list(@ParameterObject @ModelAttribute GenreSearchRequest request);

    @GetMapping("/{id}")
    @Operation(summary = "Busca um gênero pelo id", description = "Traz os ids das categorias, em ordem.")
    @ApiResponse(responseCode = OK, description = "Gênero encontrado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GenreResponse getById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um gênero",
            description = "Substitui nome e categorias; a ativação tem rotas próprias.")
    @ApiResponse(responseCode = OK, description = "Atualizado",
            content = @Content(schema = @Schema(implementation = UpdateGenreResponse.class)))
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> update(
            @Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id,
            @RequestBody UpdateGenreRequest request);
}
