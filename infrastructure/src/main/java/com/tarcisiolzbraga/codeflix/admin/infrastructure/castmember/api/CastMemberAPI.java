package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.CREATED;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.ID_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.NOT_FOUND_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Contrato HTTP e documentação OpenAPI do elenco; o CastMemberController só implementa.
@Tag(name = "Membros de elenco", description = "Cadastro dos atores e diretores do catálogo")
@RequestMapping("/cast-members")
public interface CastMemberAPI {

    @PostMapping
    @Operation(
            summary = "Cria um membro de elenco",
            description = "Sem o campo active, o membro nasce ativo. O tipo precisa ser ACTOR ou DIRECTOR.")
    @ApiResponse(responseCode = CREATED, description = "Criado; a URL do recurso vem no header Location",
            content = @Content(schema = @Schema(implementation = CreateCastMemberResponse.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> create(@RequestBody CreateCastMemberRequest request);

    @GetMapping("/{id}")
    @Operation(summary = "Busca um membro de elenco pelo id")
    @ApiResponse(responseCode = OK, description = "Membro encontrado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CastMemberResponse getById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);
}
