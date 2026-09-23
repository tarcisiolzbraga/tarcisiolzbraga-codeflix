package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.CREATED;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
}
