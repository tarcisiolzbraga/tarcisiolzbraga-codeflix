package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

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
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoSearchRequest;
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

// Contrato HTTP e documentação OpenAPI dos vídeos; o VideoController só implementa.
@Tag(name = "Vídeos", description = "Cadastro dos vídeos do catálogo")
@RequestMapping("/videos")
public interface VideoAPI {

    @PostMapping
    @Operation(
            summary = "Cria um vídeo",
            description = "Nasce fechado, não publicado e ativo. As referências informadas precisam existir.")
    @ApiResponse(responseCode = CREATED, description = "Criado; a URL do recurso vem no header Location",
            content = @Content(schema = @Schema(implementation = CreateVideoResponse.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> create(@RequestBody CreateVideoRequest request);

    @GetMapping
    @Operation(
            summary = "Lista os vídeos, paginados",
            description = "search filtra pelo título; categories, genres e castMembers filtram pelos vínculos.")
    @ApiResponse(responseCode = OK, description = "Página de vídeos")
    Pagination<VideoListResponse> list(@ParameterObject @ModelAttribute VideoSearchRequest request);

    @GetMapping("/{id}")
    @Operation(summary = "Busca um vídeo pelo id", description = "Traz os ids das referências, em ordem.")
    @ApiResponse(responseCode = OK, description = "Vídeo encontrado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse getById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um vídeo",
            description = "Substitui os dados e as referências; publicação, abertura e ativação têm rotas próprias.")
    @ApiResponse(responseCode = OK, description = "Atualizado",
            content = @Content(schema = @Schema(implementation = UpdateVideoResponse.class)))
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = UNPROCESSABLE, description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<Object> update(
            @Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id,
            @RequestBody UpdateVideoRequest request);

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publica um vídeo", description = "Idempotente: um vídeo já publicado segue publicado.")
    @ApiResponse(responseCode = OK, description = "Vídeo publicado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse publish(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "Despublica um vídeo", description = "Tira do ar sem apagar o registro.")
    @ApiResponse(responseCode = OK, description = "Vídeo despublicado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse unpublish(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/open")
    @Operation(summary = "Abre um vídeo", description = "Libera o acesso sem assinatura.")
    @ApiResponse(responseCode = OK, description = "Vídeo aberto")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse open(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/close")
    @Operation(summary = "Fecha um vídeo", description = "Volta a exigir assinatura.")
    @ApiResponse(responseCode = OK, description = "Vídeo fechado")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse close(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/activate")
    @Operation(summary = "Ativa um vídeo", description = "Idempotente: um vídeo já ativo segue ativo.")
    @ApiResponse(responseCode = OK, description = "Vídeo ativo")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse activate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Desativa um vídeo", description = "Tira do catálogo sem apagar o registro.")
    @ApiResponse(responseCode = OK, description = "Vídeo inativo")
    @ApiResponse(responseCode = NOT_FOUND, description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VideoResponse deactivate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove um vídeo",
            description = "Idempotente: id inexistente também responde 204. Os vínculos saem junto; "
                    + "categorias, gêneros e membros de elenco continuam.")
    @ApiResponse(responseCode = NO_CONTENT, description = "Removido, ou já não existia")
    void deleteById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);
}
