package com.tarcisiolzbraga.codeflix.subscription.e2e.plan;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.PlanResponse;
import java.math.BigDecimal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

// As chamadas da jornada, para o corpo do teste ler como jornada e não como montagem de requisição.
// Interface de métodos default que o teste implementa, entregando a porta e o token.
public interface PlanE2EDsl {

    int port();

    String bearer();

    default RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:%d/plans".formatted(port()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer())
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    default String createPlan(final String name, final String description, final BigDecimal price) {
        final var body = """
                {"name":"%s","description":"%s","currency":"BRL","price":%s}"""
                .formatted(name, description, price);
        final var response = client()
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(CreatedPlan.class);
        return bodyOf(response, "criar o plano").id();
    }

    default PlanResponse planOf(final String id) {
        return bodyOf(client().get().uri("/{id}", id).retrieve().toEntity(PlanResponse.class), "buscar o plano " + id);
    }

    default HttpStatusCode statusOfPlanOf(final String id) {
        return client().get().uri("/{id}", id).retrieve().toBodilessEntity().getStatusCode();
    }

    default void changePlan(final String id, final String name, final BigDecimal price) {
        final var body = """
                {"name":"%s","description":"Alterado pela jornada","currency":"BRL","price":%s}"""
                .formatted(name, price);
        client().put()
                .uri("/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    default PlanResponse activatePlan(final String id) {
        final var response =
                client().put().uri("/{id}/activate", id).retrieve().toEntity(PlanResponse.class);
        return bodyOf(response, "pôr à venda o plano " + id);
    }

    default PlanResponse deactivatePlan(final String id) {
        final var response =
                client().put().uri("/{id}/deactivate", id).retrieve().toEntity(PlanResponse.class);
        return bodyOf(response, "tirar de venda o plano " + id);
    }

    // O status handler aceita tudo, para a jornada poder afirmar sobre um 404. O preço é que uma
    // resposta de erro chegaria aqui com corpo nulo, e o teste quebraria com NullPointerException
    // sem dizer o que aconteceu. Isto troca a NPE pelo status e pelo passo que falhou.
    private static <T> T bodyOf(final ResponseEntity<T> response, final String step) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AssertionError(
                    "falhou ao %s: a API respondeu %s".formatted(step, response.getStatusCode()));
        }
        return response.getBody();
    }

    record CreatedPlan(String id) {}
}
