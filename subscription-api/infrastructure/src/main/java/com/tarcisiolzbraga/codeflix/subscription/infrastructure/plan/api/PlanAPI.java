package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.api;

import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CREATED;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.ID_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.NOT_FOUND_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.ChangePlanRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.ChangePlanResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.CreatePlanRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.CreatePlanResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.PlanResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Contrato HTTP e documentação OpenAPI dos planos; o PlanController só implementa.
@Tag(name = "Planos", description = "Os planos que o assinante pode escolher")
@RequestMapping("/plans")
public interface PlanAPI {

    @PostMapping
    @Operation(
            summary = "Cria um plano",
            description = "Sem o campo active, o plano nasce inativo: pôr um plano à venda é ato deliberado.")
    @ApiResponse(
            responseCode = CREATED,
            description = "Criado; a URL do recurso vem no header Location",
            content = @Content(schema = @Schema(implementation = CreatePlanResponse.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<CreatePlanResponse> create(@RequestBody CreatePlanRequest request);

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um plano pelo id",
            description = "Devolve o plano fora de venda também: é o operador que administra esse estado.")
    @ApiResponse(responseCode = OK, description = "Plano encontrado")
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PlanResponse getById(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}")
    @Operation(
            summary = "Altera um plano",
            description = "Substitui nome, descrição e preço; a ativação tem rotas próprias.")
    @ApiResponse(
            responseCode = OK,
            description = "Alterado",
            content = @Content(schema = @Schema(implementation = ChangePlanResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = CONFLICT,
            description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ChangePlanResponse change(
            @Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id,
            @RequestBody ChangePlanRequest request);

    @PutMapping("/{id}/activate")
    @Operation(summary = "Põe o plano à venda", description = "Idempotente: um plano já ativo segue ativo.")
    @ApiResponse(responseCode = OK, description = "Plano ativo")
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PlanResponse activate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);

    @PutMapping("/{id}/deactivate")
    @Operation(
            summary = "Tira o plano de venda",
            description = "Não apaga o registro, e não mexe em quem já assina por ele.")
    @ApiResponse(responseCode = OK, description = "Plano inativo")
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = NOT_FOUND_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PlanResponse deactivate(@Parameter(description = ID_DESCRIPTION) @PathVariable("id") String id);
}
