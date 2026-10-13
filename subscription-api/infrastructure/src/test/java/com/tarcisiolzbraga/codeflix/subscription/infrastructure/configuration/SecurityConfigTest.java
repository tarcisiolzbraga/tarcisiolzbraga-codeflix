package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.PlanOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.activate.ActivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate.DeactivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.get.GetPlanByIdUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.api.PlanController;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// A fatia web não traz @Configuration própria, então a de segurança entra por @Import. O decoder é
// construído a partir do jwk-set-uri e não busca nada na subida, então nenhum Keycloak precisa estar
// no ar — e as requisições autenticadas usam o post-processor jwt(), que não passa pelo decoder.
//
// Fixa um controller de propósito: as regras são por caminho, não por controller, então um só basta
// para exercitá-las, e agregado novo não obriga a mockar mais nada aqui. Os caminhos de conta e de
// assinatura seguem respondendo 404 porque os controllers deles não entram nesta fatia — e 404 é a
// prova de que a requisição passou pela autorização.
//
// O que depende de despacho de erro NÃO cabe aqui: o MockMvc devolve o 404 do DispatcherServlet
// direto, sem encaminhar para /error, então um teste disso passaria com e sem a correção. Esse mora
// no SecurityE2ETest.
@WebMvcTest(controllers = PlanController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "keycloak.issuer-uri=http://localhost:8081/realms/codeflix")
class SecurityConfigTest {

    private static final String ROLE_ADMIN = "ROLE_CODEFLIX_ADMIN";
    private static final String ROLE_SUBSCRIBER = "ROLE_CODEFLIX_SUBSCRIBER";
    private static final String A_PLAN = "/plans/11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreatePlanUseCase createPlanUseCase;

    @MockitoBean
    private ChangePlanUseCase changePlanUseCase;

    @MockitoBean
    private ActivatePlanUseCase activatePlanUseCase;

    @MockitoBean
    private DeactivatePlanUseCase deactivatePlanUseCase;

    @MockitoBean
    private GetPlanByIdUseCase getPlanByIdUseCase;

    @Test
    void givenNoToken_whenCallsAPlan_thenIsUnauthorized() throws Exception {
        final var request = get(A_PLAN);

        final var actual = mvc.perform(request);

        actual.andExpect(status().isUnauthorized());
    }

    @Test
    void givenSubscriberToken_whenCallsAPlan_thenIsForbiddenBecausePlanIsAdministration() throws Exception {
        final var request = get(A_PLAN).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_SUBSCRIBER)));

        final var actual = mvc.perform(request);

        actual.andExpect(status().isForbidden());
    }

    // Passa pela autorização e chega ao handler: 200, e não 404 por ausência de rota.
    @Test
    void givenAdminToken_whenCallsAPlan_thenReachesTheHandler() throws Exception {
        when(getPlanByIdUseCase.execute(any())).thenReturn(aPlanOutput());
        final var request = get(A_PLAN).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ADMIN)));

        final var actual = mvc.perform(request);

        actual.andExpect(status().isOk());
    }

    @Test
    void givenSubscriberToken_whenCallsSubscriptions_thenIsLetThrough() throws Exception {
        final var request =
                get("/subscriptions").with(jwt().authorities(new SimpleGrantedAuthority(ROLE_SUBSCRIBER)));

        final var actual = mvc.perform(request);

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenNoToken_whenSignsUp_thenIsLetThroughBecauseThereIsNoTokenToHaveYet() throws Exception {
        final var request = post("/accounts/sign-up");

        final var actual = mvc.perform(request);

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenNoToken_whenCallsTheErrorPathDirectly_thenIsUnauthorized() throws Exception {
        final var request = get("/error");

        final var actual = mvc.perform(request);

        actual.andExpect(status().isUnauthorized());
    }

    @Test
    void givenNoToken_whenCallsAnUnlistedPath_thenIsUnauthorizedBeforeRevealingItDoesNotExist() throws Exception {
        final var request = get("/nao-existe");

        final var actual = mvc.perform(request);

        actual.andExpect(status().isUnauthorized());
    }

    private static PlanOutput aPlanOutput() {
        return new PlanOutput(
                "11111111-1111-1111-1111-111111111111",
                "Plus",
                "Quatro telas",
                true,
                "BRL",
                new BigDecimal("49.90"),
                Instant.EPOCH,
                Instant.EPOCH);
    }
}
