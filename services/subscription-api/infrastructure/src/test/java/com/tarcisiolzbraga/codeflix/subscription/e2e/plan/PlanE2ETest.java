package com.tarcisiolzbraga.codeflix.subscription.e2e.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.KeycloakTestToken;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

// A jornada do operador sobre a API de verdade: Tomcat, MySQL e Keycloak em container, e um
// RestClient batendo como qualquer cliente. O estado é criado pela própria API e lido de volta por
// ela — nada é semeado no banco por baixo.
//
// Não repete asserção de contrato: status e corpo ficam no PlanControllerTest e nos testes de JSON,
// e o comportamento do SQL no PlanJdbcGatewayIT.
@E2ETest
class PlanE2ETest implements PlanE2EDsl {

    private static final String NAME = "Plus";
    private static final String DESCRIPTION = "Quatro telas";
    private static final String NEW_NAME = "Plus Anual";
    private static final BigDecimal PRICE = new BigDecimal("49.90");

    @LocalServerPort
    private int port;

    @Autowired
    private KeycloakTestToken token;

    @Override
    public int port() {
        return this.port;
    }

    @Override
    public String bearer() {
        return this.token.bearer();
    }

    @Test
    void givenAnOperator_whenCreatesAPlan_thenItCanReadItBackByTheApi() {
        final var id = createPlan(NAME, DESCRIPTION, PRICE);

        final var actual = planOf(id);

        assertNotNull(actual);
        assertEquals(NAME, actual.name());
        assertEquals(0, PRICE.compareTo(actual.price()));
    }

    // Nasce fora de venda: pôr um plano à venda é ato deliberado.
    @Test
    void givenAnOperator_whenCreatesAPlanWithoutSayingActive_thenItIsBornOutOfSale() {
        final var id = createPlan(NAME, DESCRIPTION, PRICE);

        final var actual = planOf(id);

        assertFalse(actual.active());
    }

    @Test
    void givenAPlanOutOfSale_whenTheOperatorActivatesIt_thenTheApiShowsItOnSale() {
        final var id = createPlan(NAME, DESCRIPTION, PRICE);

        activatePlan(id);

        assertTrue(planOf(id).active());
    }

    @Test
    void givenAPlanOnSale_whenTheOperatorTakesItOut_thenTheApiShowsItOutOfSale() {
        final var id = createPlan(NAME, DESCRIPTION, PRICE);
        activatePlan(id);

        deactivatePlan(id);

        assertFalse(planOf(id).active());
    }

    @Test
    void givenAPlan_whenTheOperatorChangesIt_thenTheApiShowsTheNewValues() {
        final var id = createPlan(NAME, DESCRIPTION, PRICE);
        final var newPrice = new BigDecimal("59.90");

        changePlan(id, NEW_NAME, newPrice);

        final var actual = planOf(id);
        assertEquals(NEW_NAME, actual.name());
        assertEquals(0, newPrice.compareTo(actual.price()));
    }

    // Alterar não mexe em venda: o estado sobrevive à alteração dos dados.
    @Test
    void givenAPlanOnSale_whenTheOperatorChangesItsData_thenItStaysOnSale() {
        final var id = createPlan(NAME, DESCRIPTION, PRICE);
        activatePlan(id);

        changePlan(id, NEW_NAME, PRICE);

        assertTrue(planOf(id).active());
    }

    @Test
    void givenNoSuchPlan_whenTheOperatorAsksForIt_thenTheApiSaysItIsNotThere() {
        final var unknown = UUID.randomUUID().toString();

        final var actual = statusOfPlanOf(unknown);

        assertEquals(HttpStatus.NOT_FOUND, actual);
    }
}
