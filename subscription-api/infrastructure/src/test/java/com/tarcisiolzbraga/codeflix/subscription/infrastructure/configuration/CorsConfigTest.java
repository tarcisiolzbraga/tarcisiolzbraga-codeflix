package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.activate.ActivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate.DeactivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.get.GetPlanByIdUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.ListPlansOnSaleUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.ListPlansUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.api.PlanController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// A política de CORS, na mesma fatia web do SecurityConfigTest e importando as duas configurações de
// verdade — é a cadeia de filtros montada pelo serviço que responde aqui, não uma simulada.
//
// Vale para os três serviços: o CorsConfig é a mesma classe nos três, e o CopiedSourcesTest prova que
// continua sendo. Aqui mora a política; a **ligação** de cada serviço é exercitada no SecurityE2ETest
// dele, com Tomcat e filtro de verdade.
//
// A origem de teste é a do perfil development, que é o que vale quando não há perfil ativo:
// http://localhost:3000.
@WebMvcTest(controllers = PlanController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(
        properties = {
            "keycloak.issuer-uri=http://localhost:8081/realms/codeflix",
            // Duas origens, como na homologação: é o que prova que a lista é quebrada por vírgula.
            "cors.allowed-origins=http://localhost:3000,https://h.tarcisiolzbraga.codeflix.com"
        })
class CorsConfigTest {

    private static final String FRONT = "http://localhost:3000";
    private static final String FRONT_DA_HOMOLOGACAO = "https://h.tarcisiolzbraga.codeflix.com";
    private static final String OUTRA_ORIGEM = "https://nao-e-o-nosso-front.com";
    private static final String A_PLAN = "/plans/11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreatePlanUseCase createPlanUseCase;

    @MockitoBean
    private ListPlansOnSaleUseCase listPlansOnSaleUseCase;

    @MockitoBean
    private ListPlansUseCase listPlansUseCase;

    @MockitoBean
    private ChangePlanUseCase changePlanUseCase;

    @MockitoBean
    private ActivatePlanUseCase activatePlanUseCase;

    @MockitoBean
    private DeactivatePlanUseCase deactivatePlanUseCase;

    @MockitoBean
    private GetPlanByIdUseCase getPlanByIdUseCase;

    // A verificação prévia do browser passa **sem token**, e é o que torna o CORS utilizável: ela sai
    // antes de qualquer tela ter um token, e nenhuma regra de rota precisou liberar OPTIONS para isso
    // — quem responde é o filtro de CORS, antes da autorização.
    @Test
    void givenTheFrontOrigin_whenAsksPreflightWithoutToken_thenIsAllowed() throws Exception {
        final var actual = mvc.perform(options(A_PLAN)
                .header(HttpHeaders.ORIGIN, FRONT)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"));

        actual.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    // A **segunda** origem da lista, e é por isso que este teste existe: `@Value` com List<String>
    // só quebra o texto por vírgula porque o Boot instala o ApplicationConversionService. Num
    // contexto sem ele a mesma anotação entrega a lista inteira como um elemento só — conferido — e
    // aí a homologação, que tem duas origens, recusaria as duas. Se alguém trocar a leitura da
    // propriedade, é este teste que acusa.
    @Test
    void givenTheSecondOriginOfTheList_whenAsksPreflight_thenIsAllowedToo() throws Exception {
        final var actual = mvc.perform(options(A_PLAN)
                .header(HttpHeaders.ORIGIN, FRONT_DA_HOMOLOGACAO)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"));

        actual.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_DA_HOMOLOGACAO));
    }

    // Origem que não está na lista é recusada na própria verificação prévia, sem chegar à rota.
    @Test
    void givenAnotherOrigin_whenAsksPreflight_thenIsRefused() throws Exception {
        final var actual = mvc.perform(options(A_PLAN)
                .header(HttpHeaders.ORIGIN, OUTRA_ORIGEM)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"));

        actual.andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // **O CORS não é uma porta de entrada.** Liberar a origem só faz o browser deixar o front ler a
    // resposta; quem autoriza continua sendo o token, e sem ele a resposta é 401 como para qualquer
    // um. Este é o teste que impediria alguém de "resolver" um 401 mexendo no CORS.
    //
    // Diferente dos outros três, ele passa **com e sem** o .cors() na cadeia, e isso é o desenho dele:
    // ele vigia a direção oposta — que ligar o CORS não abriu nada. Conferido tirando a ligação: os
    // outros três ficam vermelhos, este continua verde.
    @Test
    void givenTheFrontOrigin_whenCallsWithoutToken_thenIsStillUnauthorized() throws Exception {
        final var actual = mvc.perform(get(A_PLAN).header(HttpHeaders.ORIGIN, FRONT));

        actual.andExpect(status().isUnauthorized());
    }

    // O Location é exposto porque o browser esconde cabeçalho de resposta que não seja simples, e as
    // criações desta API respondem 201 apontando para o recurso criado.
    @Test
    void givenTheFrontOrigin_whenAsksPreflightForACreation_thenLocationIsExposed() throws Exception {
        final var actual = mvc.perform(options("/plans")
                .header(HttpHeaders.ORIGIN, FRONT)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        actual.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION));
    }


    // `CORS_ALLOWED_ORIGINS=` no ambiente — definida e vazia — não é o mesmo que ausente: o padrão do
    // placeholder não vale, e a lista chega vazia. Sem a conferência, a aplicação subiria com uma
    // política que recusa **toda** origem, e o defeito apareceria só no browser de quem fosse usar.
    //
    // Afirmar isso no README exige provar como o Spring converte texto vazio em lista, e é o que este
    // teste faz: sobe um contexto só com o CorsConfig e afirma que ele não sobe.
    @Test
    void givenAnEmptyOriginProperty_whenTheContextStarts_thenItFailsToStart() {
        final var actual = bootLikeContext().withPropertyValues("cors.allowed-origins=");

        actual.run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(
                    rootCauseMessageOf(context.getStartupFailure()).contains("cors.allowed-origins"),
                    rootCauseMessageOf(context.getStartupFailure()));
        });
    }

    // E origem em branco no meio da lista também: "http://localhost:3000, " é erro de digitação, não
    // uma origem.
    @Test
    void givenABlankOriginInTheList_whenTheContextStarts_thenItFailsToStart() {
        final var actual = bootLikeContext().withPropertyValues("cors.allowed-origins=http://localhost:3000, ,");

        actual.run(context -> assertNotNull(context.getStartupFailure()));
    }

    // Com a conversão que o Boot instala, e não a do contexto cru: sem ela o `@Value` nem quebra a
    // lista por vírgula, e estes dois testes passariam exercitando outro caminho que não o de produção.
    private static ApplicationContextRunner bootLikeContext() {
        return new ApplicationContextRunner()
                .withInitializer(context ->
                        context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(CorsConfig.class);
    }

    private static String rootCauseMessageOf(final Throwable failure) {
        var cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return String.valueOf(cause.getMessage());
    }
}
