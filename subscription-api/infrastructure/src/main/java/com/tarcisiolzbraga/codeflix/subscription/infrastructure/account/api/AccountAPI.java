package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.api;

import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

// Contrato HTTP e documentação OpenAPI do cadastro; o AccountController só implementa.
@Tag(name = "Contas", description = "Cadastro de quem assina")
@RequestMapping("/accounts")
public interface AccountAPI {

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Pede o cadastro",
            description = "Responde 202 e a MESMA mensagem em todos os casos — e-mail novo, e-mail que já existe "
                    + "no provedor de identidade ou e-mail que já tem conta. Resposta diferente contaria a um "
                    + "estranho se um endereço está cadastrado. A conta só nasce quando o e-mail for confirmado.")
    @ApiResponse(
            responseCode = "202",
            description = "Pedido aceito; um e-mail de validação foi posto na fila",
            content = @Content(schema = @Schema(implementation = SignUpResponse.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    SignUpResponse signUp(@RequestBody SignUpRequest request);

    @PostMapping("/sign-up/confirm")
    @Operation(
            summary = "Confirma o cadastro com o token do e-mail",
            description = "É aqui que a conta nasce. Chamar de novo com o mesmo token devolve a mesma conta, "
                    + "em vez de erro: clicar no link duas vezes é comum.")
    @ApiResponse(
            responseCode = OK,
            description = "Conta criada, ou a que já havia sido criada por esta mesma confirmação",
            content = @Content(schema = @Schema(implementation = ConfirmSignUpResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "O link não existe ou não vale mais",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = CONFLICT,
            description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = "O link venceu, ou o token está mal formado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ConfirmSignUpResponse confirmSignUp(@RequestBody ConfirmSignUpRequest request);
}
