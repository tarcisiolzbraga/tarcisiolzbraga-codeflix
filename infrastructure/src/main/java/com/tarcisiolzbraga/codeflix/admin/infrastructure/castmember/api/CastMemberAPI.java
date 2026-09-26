package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

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
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberSearchRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.UpdateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.UpdateCastMemberResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @GetMapping
    @Operation(summary = "Lista os membros de elenco, paginados", description = "search filtra pelo nome.")
    @ApiResponse(responseCode = OK, description = "Página de membros de elenco")
    Pagination<CastMemberListResponse> list(@ParameterObject @ModelAttribute CastMemberSearchRequest request);

    @GetMapping("/{id}")
    @Operation(summary = "Busca um membro de elenco pelo id")
    @ApiResponse(responseCode = OK, description = "Membro encontrado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CastMemberResponse getById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um membro de elenco",
            description = "Substitui nome e tipo; a ativação tem rotas próprias.")
    @ApiResponse(responseCode = OK, description = "Atualizado",
            content = @Content(schema = @Schema(implementation = UpdateCastMemberResponse.class)))
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> update(
            @Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id,
            @RequestBody UpdateCastMemberRequest request);

    @PutMapping("/{id}/activate")
    @Operation(summary = "Ativa um membro de elenco", description = "Idempotente: um membro já ativo segue ativo.")
    @ApiResponse(responseCode = OK, description = "Membro ativo")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CastMemberResponse activate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Desativa um membro de elenco", description = "Tira do catálogo sem apagar o registro.")
    @ApiResponse(responseCode = OK, description = "Membro inativo")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CastMemberResponse deactivate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove um membro de elenco",
            description = "Idempotente: id inexistente também responde 204.")
    @ApiResponse(responseCode = NO_CONTENT, description = "Removido, ou já não existia")
    void deleteById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);
}
