package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.api;

import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CONFLICT_DESCRIPTION;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.CREATED;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.NOT_FOUND;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.OK;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE;
import static com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiResponseDocs.UNPROCESSABLE_DESCRIPTION;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.ChargeRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.ChargeResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.SubscribeRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Contrato HTTP e documentação OpenAPI da assinatura; o SubscriptionController só implementa.
//
// Toda rota daqui é sobre a assinatura de quem está chamando, identificada pelo token — por isso
// `/me`, e por isso nenhuma delas recebe id de assinatura: não existe caminho para pedir a de outro.
@Tag(name = "Assinaturas", description = "A assinatura de quem está chamando")
@RequestMapping("/subscriptions")
public interface SubscriptionAPI {

    @PostMapping
    @Operation(
            summary = "Assina um plano",
            description = "A assinatura nasce em período de experiência, vencendo em um mês, e sem cobrança: "
                    + "cobrar é ato separado. Quem já tem assinatura viva recebe 409 — para trocar de plano há "
                    + "rota própria.")
    @ApiResponse(
            responseCode = CREATED,
            description = "Assinado; a URL do recurso vem no header Location",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "O token é válido, mas quem o apresenta não tem conta aqui, ou o plano não existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = CONFLICT,
            description = "Quem está chamando já tem uma assinatura que não foi cancelada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = "O plano está fora de venda, ou o id do plano está mal formado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ResponseEntity<SubscriptionResponse> subscribe(Principal caller, @RequestBody SubscribeRequest request);

    @GetMapping("/me")
    @Operation(
            summary = "A assinatura de quem está chamando",
            description = "Devolve a assinatura cancelada também: quem cancelou ainda precisa poder ver o que "
                    + "aconteceu com ela.")
    @ApiResponse(
            responseCode = OK,
            description = "A assinatura",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "Quem está chamando nunca assinou",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    SubscriptionResponse mySubscription(Principal caller);

    // Hoje a cobrança é pedida; amanhã um job varre as vencidas e chama o mesmo caso de uso. A rota
    // fica de qualquer forma: é o que permite a quem teve o cartão recusado pagar com outro cartão
    // sem esperar a próxima varredura.
    @PostMapping("/me/charge")
    @Operation(
            summary = "Cobra a assinatura vencida",
            description = "Responde 200 mesmo quando o provedor recusa o cartão: a requisição foi atendida, e o "
                    + "resultado da tentativa está no corpo, com o motivo. Assinatura que não venceu responde "
                    + "charged=false sem tocar no provedor. Recusada, a assinatura fica incompleta; passada a "
                    + "tolerância, é cancelada.")
    @ApiResponse(
            responseCode = OK,
            description = "A tentativa foi feita; charged diz se o pagamento passou",
            content = @Content(schema = @Schema(implementation = ChargeResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "Quem está chamando nunca assinou",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = CONFLICT,
            description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = "A conta não tem endereço de cobrança, a assinatura está cancelada, ou falta o token "
                    + "do cartão",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ChargeResponse charge(Principal caller, @RequestBody ChargeRequest request);

    @PutMapping("/me/cancel")
    @Operation(
            summary = "Cancela a assinatura",
            description = "Cancelar de novo devolve a mesma assinatura cancelada, em vez de erro: o fim é o "
                    + "mesmo, e o pedido já foi atendido.")
    @ApiResponse(
            responseCode = OK,
            description = "A assinatura, já cancelada",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    @ApiResponse(
            responseCode = NOT_FOUND,
            description = "Quem está chamando nunca assinou",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = CONFLICT,
            description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = UNPROCESSABLE,
            description = UNPROCESSABLE_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    SubscriptionResponse cancel(Principal caller);
}
