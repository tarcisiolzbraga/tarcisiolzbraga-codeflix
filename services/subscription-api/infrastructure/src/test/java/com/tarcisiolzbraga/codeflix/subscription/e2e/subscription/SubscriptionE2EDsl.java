package com.tarcisiolzbraga.codeflix.subscription.e2e.subscription;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.ChargeResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.SubscriptionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

// As chamadas da jornada da assinatura, para o corpo do teste ler como jornada.
//
// Cada método recebe o bearer de quem chama, em vez de a interface guardar um: a jornada tem três
// papéis — o operador que publica o plano, a pessoa que assina e, em um dos testes, outra pessoa.
public interface SubscriptionE2EDsl {

    int port();

    default RestClient client(final String bearer) {
        return RestClient.builder()
                .baseUrl("http://localhost:%d".formatted(port()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    default String publishPlan(final String operator, final String name, final String price) {
        final var body = """
                {"name":"%s","description":"Plano da jornada","currency":"BRL","price":%s}"""
                .formatted(name, price);
        final var created = client(operator)
                .post()
                .uri("/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(CreatedPlan.class);
        final var id = bodyOf(created, "criar o plano").id();
        client(operator).put().uri("/plans/{id}/activate", id).retrieve().toBodilessEntity();
        return id;
    }

    default void setBillingAddress(final String subscriber) {
        final var body =
                """
                {"zipcode":"01310930","number":"1578","complement":"apto 42","country":"BR"}""";
        final var response = client(subscriber)
                .put()
                .uri("/accounts/me/billing-address")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("falhou ao gravar o endereço: a API respondeu " + response.getStatusCode());
        }
    }

    default ResponseEntity<SubscriptionResponse> subscribeTo(final String subscriber, final String planId) {
        return client(subscriber)
                .post()
                .uri("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"planId\":\"%s\"}".formatted(planId))
                .retrieve()
                .toEntity(SubscriptionResponse.class);
    }

    default SubscriptionResponse mySubscription(final String subscriber) {
        return bodyOf(
                client(subscriber).get().uri("/subscriptions/me").retrieve().toEntity(SubscriptionResponse.class),
                "ler a própria assinatura");
    }

    default HttpStatusCode statusOfMySubscription(final String subscriber) {
        return client(subscriber)
                .get()
                .uri("/subscriptions/me")
                .retrieve()
                .toBodilessEntity()
                .getStatusCode();
    }

    default ChargeResponse chargeWith(final String subscriber, final String creditCardToken) {
        final var response = client(subscriber)
                .post()
                .uri("/subscriptions/me/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"creditCardToken\":\"%s\"}".formatted(creditCardToken))
                .retrieve()
                .toEntity(ChargeResponse.class);
        return bodyOf(response, "cobrar a assinatura");
    }

    default SubscriptionResponse cancelMySubscription(final String subscriber) {
        return bodyOf(
                client(subscriber)
                        .put()
                        .uri("/subscriptions/me/cancel")
                        .retrieve()
                        .toEntity(SubscriptionResponse.class),
                "cancelar a assinatura");
    }

    // O status handler aceita tudo, para a jornada poder afirmar sobre um 404 ou um 409. O preço é
    // que uma resposta de erro chegaria aqui com corpo nulo, e o teste quebraria com
    // NullPointerException sem dizer o que aconteceu. Isto troca a NPE pelo status e pelo passo.
    private static <T> T bodyOf(final ResponseEntity<T> response, final String step) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AssertionError("falhou ao %s: a API respondeu %s".formatted(step, response.getStatusCode()));
        }
        return response.getBody();
    }

    record CreatedPlan(String id) {}
}
