package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// A fatia web não traz @Configuration própria, então a de segurança entra por @Import. O decoder é
// construído a partir do jwk-set-uri e não busca nada na subida, então nenhum Keycloak precisa estar
// no ar — e as requisições autenticadas usam o post-processor jwt(), que não passa pelo decoder.
//
// Aqui se verifica quem alcança o quê: 401 sem token, 403 com a role errada, e passa com a certa —
// o 404 de quem passa é a ausência de handler, não recusa. O que depende de despacho de erro NÃO
// cabe nesta fatia: o MockMvc devolve o 404 do DispatcherServlet direto, sem encaminhar para /error,
// então um teste disso aqui passaria com e sem a correção. Esse mora no SecurityE2ETest.
@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = "keycloak.issuer-uri=http://localhost:8081/realms/codeflix")
class SecurityConfigTest {

    private static final String ROLE_ADMIN = "ROLE_CODEFLIX_ADMIN";
    private static final String ROLE_SUBSCRIBER = "ROLE_CODEFLIX_SUBSCRIBER";

    @Autowired
    private MockMvc mvc;

    @Test
    void givenNoToken_whenCallsPlans_thenIsUnauthorized() throws Exception {
        final var request = get("/plans");

        final var actual = mvc.perform(request);

        actual.andExpect(status().isUnauthorized());
    }

    @Test
    void givenSubscriberToken_whenCallsPlans_thenIsForbiddenBecausePlanIsAdministration() throws Exception {
        final var request = get("/plans").with(jwt().authorities(new SimpleGrantedAuthority(ROLE_SUBSCRIBER)));

        final var actual = mvc.perform(request);

        actual.andExpect(status().isForbidden());
    }

    @Test
    void givenSubscriberToken_whenCallsSubscriptions_thenIsAuthorized() throws Exception {
        final var request =
                get("/subscriptions").with(jwt().authorities(new SimpleGrantedAuthority(ROLE_SUBSCRIBER)));

        final var actual = mvc.perform(request);

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenAdminToken_whenCallsPlans_thenIsAuthorized() throws Exception {
        final var request = get("/plans").with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ADMIN)));

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
}
