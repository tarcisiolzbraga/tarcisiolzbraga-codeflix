package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.api;

import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.TOO_MANY_REQUESTS;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.TOO_MANY_REQUESTS_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.AccountResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.BillingAddressRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.PaymentMethodRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @ApiResponse(
            responseCode = TOO_MANY_REQUESTS,
            description = TOO_MANY_REQUESTS_DESCRIPTION,
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
    @ApiResponse(
            responseCode = TOO_MANY_REQUESTS,
            description = TOO_MANY_REQUESTS_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ConfirmSignUpResponse confirmSignUp(@RequestBody ConfirmSignUpRequest request);

    // `/me` em vez de `/{id}`: com id no caminho, um assinante poderia pedir a conta de outro, e
    // toda rota daqui teria de conferir propriedade. A conta sai do `sub` do token, que é o mesmo id
    // do provedor que a conta guarda, e a pergunta "esta conta é sua?" deixa de existir.
    @GetMapping("/me")
    @Operation(summary = "A conta de quem está chamando", description = "Identificada pelo token, não por id.")
    @ApiResponse(
            responseCode = OK,
            description = "A conta",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "O token é válido, mas quem o apresenta não tem conta aqui",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AccountResponse myAccount(Principal caller);

    @PutMapping("/me/billing-address")
    @Operation(
            summary = "Troca o endereço de cobrança",
            description = "Substitui o endereço inteiro. Só o complemento é opcional.")
    @ApiResponse(
            responseCode = OK,
            description = "A conta, já com o endereço novo",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "O token é válido, mas quem o apresenta não tem conta aqui",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AccountResponse changeBillingAddress(Principal caller, @RequestBody BillingAddressRequest request);

    // É esta rota que torna a cobrança automática possível: sem meio de pagamento guardado, só se
    // cobra quem está presente com o cartão na mão.
    @PutMapping("/me/payment-method")
    @Operation(
            summary = "Guarda o meio de pagamento",
            description = "Recebe o token de uso único que o navegador obteve do provedor, troca por uma "
                    + "referência durável e guarda **só a referência**. Nenhum dado de cartão, nem o token de "
                    + "uso único, fica neste serviço. Chamar de novo substitui o meio guardado.")
    @ApiResponse(
            responseCode = OK,
            description = "A conta, já com o meio de pagamento guardado",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "O token é válido, mas quem o apresenta não tem conta aqui",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = "Falta o token do cartão, ou o provedor o recusou",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AccountResponse storePaymentMethod(Principal caller, @RequestBody PaymentMethodRequest request);
}
