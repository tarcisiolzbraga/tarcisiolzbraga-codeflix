package com.tarcisiolzbraga.codeflix.subscription.e2e.account;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.models.SignUpValidationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// As chamadas da jornada do cadastro, para o corpo do teste ler como jornada.
//
// Inclui ler a fila, porque o token só existe no e-mail: é a única forma de a jornada seguir o
// mesmo caminho que uma pessoa segue.
public interface SignUpE2EDsl {

    int port();

    RabbitTemplate rabbit();

    ObjectMapper mapper();

    String validationQueue();

    default RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:%d/accounts".formatted(port()))
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    default HttpStatusCode signUp(final String email, final String document) {
        final var body = """
                {"firstname":"Tarcisio","lastname":"Braga","email":"%s","password":"uma-senha-boa",\
                "documentNumber":"%s","documentType":"cpf"}"""
                .formatted(email, document);
        return client().post()
                .uri("/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode();
    }

    default Confirmation confirm(final String token) {
        final var response = client().post()
                .uri("/sign-up/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"token\":\"%s\"}".formatted(token))
                .retrieve()
                .toEntity(ConfirmedAccount.class);
        return new Confirmation(
                response.getStatusCode(), response.getBody() == null ? null : response.getBody().accountId());
    }

    // O token como a pessoa o recebe: tirado do link da mensagem que foi para a fila.
    default String tokenFromTheEmail() {
        final var message = rabbit().receive(validationQueue(), 5_000L);
        if (message == null) {
            throw new AssertionError("nenhum e-mail de validação chegou na fila " + validationQueue());
        }
        final var payload = mapper().readValue(new String(message.getBody()), SignUpValidationMessage.class);
        final var url = payload.confirmationUrl();
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    default boolean noEmailWasSent() {
        return rabbit().receive(validationQueue(), 500L) == null;
    }

    default void drainTheQueue() {
        while (rabbit().receive(validationQueue(), 100L) != null) {
            // descarta o que sobrou de outro teste
        }
    }

    record Confirmation(HttpStatusCode status, String accountId) {}

    record ConfirmedAccount(String accountId) {}
}
