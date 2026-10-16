package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.PlanListOutput;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;
import java.util.List;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.ListPlansOnSaleUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.ListPlansUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.PlanOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.activate.ActivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate.DeactivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.get.GetPlanByIdUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ControllerTest;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = PlanController.class)
class PlanControllerTest {

    private static final String PLANS_PATH = "/plans";
    private static final String EXPECTED_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CREATE_BODY =
            """
            {"name":"Plus","description":"Quatro telas","active":true,"currency":"BRL","price":49.90}""";
    private static final String CHANGE_BODY =
            """
            {"name":"Básico","description":"Uma tela","currency":"BRL","price":19.90}""";

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

    // O caminho literal tem de vencer o de template: sem isso, `/plans/on-sale` cairia no
    // `/plans/{id}` com id "on-sale", e o catálogo responderia 422 por id mal formado. O Spring
    // prefere o literal, e este teste fixa isso — é o tipo de coisa que uma versão nova poderia
    // mudar sem avisar.
    @Test
    void givenTheCatalogPath_whenCalled_thenItDoesNotFallIntoTheGetByIdRoute() throws Exception {
        when(listPlansOnSaleUseCase.execute(any())).thenReturn(emptyPage());

        final var actual = mvc.perform(get(PLANS_PATH + "/on-sale"));

        actual.andExpect(status().isOk());
        verify(getPlanByIdUseCase, never()).execute(any());
    }

    // Os parâmetros da query chegam ao caso de uso. Sem este teste, trocar o nome de um deles faria
    // a paginação voltar ao padrão em silêncio.
    @Test
    void givenPageAndPerPage_whenListsTheCatalog_thenTheyReachTheUseCase() throws Exception {
        when(listPlansOnSaleUseCase.execute(any())).thenReturn(emptyPage());

        mvc.perform(get(PLANS_PATH + "/on-sale").param("page", "2").param("perPage", "5"));

        verify(listPlansOnSaleUseCase).execute(argThat(page -> page.page() == 2 && page.perPage() == 5));
    }

    // Sem parâmetro nenhum a rota funciona: página zero, tamanho padrão.
    @Test
    void givenNoParameters_whenListsTheCatalog_thenTheFirstPageIsAsked() throws Exception {
        when(listPlansOnSaleUseCase.execute(any())).thenReturn(emptyPage());

        mvc.perform(get(PLANS_PATH + "/on-sale"));

        verify(listPlansOnSaleUseCase).execute(argThat(page -> page.page() == 0 && page.perPage() == 20));
    }

    // Teto do tamanho da página: um cliente não pede a base inteira numa página.
    @Test
    void givenAPerPageAboveTheCap_whenListsTheCatalog_thenItIsCappedBeforeTheUseCase() throws Exception {
        when(listPlansOnSaleUseCase.execute(any())).thenReturn(emptyPage());

        mvc.perform(get(PLANS_PATH + "/on-sale").param("perPage", "5000"));

        verify(listPlansOnSaleUseCase).execute(argThat(page -> page.perPage() == 100));
    }

    @Test
    void givenPlansOnSale_whenListsTheCatalog_thenTheBodyCarriesThePageAndTheItems() throws Exception {
        when(listPlansOnSaleUseCase.execute(any()))
                .thenReturn(new Pagination<>(0, 20, 3, List.of(anOutput(true))));

        final var actual = mvc.perform(get(PLANS_PATH + "/on-sale"));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.perPage").value(20))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].name").value("Plus"))
                .andExpect(jsonPath("$.items[0].price").value(49.90));
    }

    // O catálogo aberto não diz `active`: todos ali estão à venda, e campo que nunca varia é ruído.
    @Test
    void givenPlansOnSale_whenListsTheCatalog_thenTheItemsDoNotCarryActive() throws Exception {
        when(listPlansOnSaleUseCase.execute(any()))
                .thenReturn(new Pagination<>(0, 20, 1, List.of(anOutput(true))));

        final var actual = mvc.perform(get(PLANS_PATH + "/on-sale"));

        actual.andExpect(jsonPath("$.items[0].active").doesNotExist());
    }

    // A listagem do operador diz, porque ali ele varia.
    @Test
    void givenAPlanOutOfSale_whenTheOperatorLists_thenTheItemSaysItIsNotActive() throws Exception {
        when(listPlansUseCase.execute(any())).thenReturn(new Pagination<>(0, 20, 1, List.of(anOutput(false))));

        final var actual = mvc.perform(get(PLANS_PATH));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].active").value(false));
    }

    private static Pagination<PlanListOutput> emptyPage() {
        return new Pagination<>(0, 20, 0, List.of());
    }

    private static PlanListOutput anOutput(final boolean active) {
        return new PlanListOutput(
                EXPECTED_ID, "Plus", "Quatro telas", "BRL", new java.math.BigDecimal("49.90"), active);
    }

    @Test
    void givenAValidBody_whenCreates_thenAnswersCreatedWithTheLocation() throws Exception {
        when(createPlanUseCase.execute(any())).thenReturn(new CreatePlanOutput(EXPECTED_ID));

        final var actual = mvc.perform(post(PLANS_PATH).contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY));

        actual.andExpect(status().isCreated())
                .andExpect(header().string("Location", PLANS_PATH + "/" + EXPECTED_ID))
                .andExpect(jsonPath("$.id").value(EXPECTED_ID));
    }

    // argThat, e não any(): sem isto renomear um campo do request faria o valor chegar nulo ao
    // command e o teste continuaria verde.
    @Test
    void givenAValidBody_whenCreates_thenTheCommandCarriesTheValuesFromTheBody() throws Exception {
        when(createPlanUseCase.execute(any())).thenReturn(new CreatePlanOutput(EXPECTED_ID));

        mvc.perform(post(PLANS_PATH).contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY));

        verify(createPlanUseCase).execute(argThat(command -> "Plus".equals(command.name())
                && "Quatro telas".equals(command.description())
                && Boolean.TRUE.equals(command.active())
                && "BRL".equals(command.currency())
                && new BigDecimal("49.90").compareTo(command.price()) == 0));
    }

    @Test
    void givenABodyWithoutActive_whenCreates_thenTheCommandSaysInactive() throws Exception {
        final var body = """
                {"name":"Plus","description":"Quatro telas","currency":"BRL","price":49.90}""";
        when(createPlanUseCase.execute(any())).thenReturn(new CreatePlanOutput(EXPECTED_ID));

        mvc.perform(post(PLANS_PATH).contentType(MediaType.APPLICATION_JSON).content(body));

        verify(createPlanUseCase).execute(argThat(command -> Boolean.FALSE.equals(command.active())));
    }

    @Test
    void givenDataTheDomainRefuses_whenCreates_thenAnswersUnprocessableWithTheMessage() throws Exception {
        doThrow(DomainException.with("'name' should not be empty")).when(createPlanUseCase).execute(any());

        final var actual = mvc.perform(post(PLANS_PATH).contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("'name' should not be empty"));
    }

    @Test
    void givenAValidBody_whenChanges_thenAnswersOkWithTheId() throws Exception {
        when(changePlanUseCase.execute(any())).thenReturn(new ChangePlanOutput(EXPECTED_ID));

        final var actual = mvc.perform(
                put(PLANS_PATH + "/" + EXPECTED_ID).contentType(MediaType.APPLICATION_JSON).content(CHANGE_BODY));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(EXPECTED_ID));
    }

    @Test
    void givenAValidBody_whenChanges_thenTheCommandCarriesTheIdFromThePathAndTheBodyValues() throws Exception {
        when(changePlanUseCase.execute(any())).thenReturn(new ChangePlanOutput(EXPECTED_ID));

        mvc.perform(put(PLANS_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_BODY));

        verify(changePlanUseCase).execute(argThat(command -> EXPECTED_ID.equals(command.planId())
                && "Básico".equals(command.name())
                && "Uma tela".equals(command.description())
                && new BigDecimal("19.90").compareTo(command.price()) == 0));
    }

    @Test
    void givenAMissingPlan_whenChanges_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.with(Plan.class, PlanId.from(EXPECTED_ID)))
                .when(changePlanUseCase)
                .execute(any());

        final var actual = mvc.perform(
                put(PLANS_PATH + "/" + EXPECTED_ID).contentType(MediaType.APPLICATION_JSON).content(CHANGE_BODY));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenAConcurrentWrite_whenChanges_thenAnswersConflict() throws Exception {
        doThrow(ConflictException.staleVersion(Plan.class, PlanId.from(EXPECTED_ID)))
                .when(changePlanUseCase)
                .execute(any());

        final var actual = mvc.perform(
                put(PLANS_PATH + "/" + EXPECTED_ID).contentType(MediaType.APPLICATION_JSON).content(CHANGE_BODY));

        actual.andExpect(status().isConflict());
    }

    @Test
    void givenAnExistingPlan_whenActivates_thenAnswersOkWithTheWholePlan() throws Exception {
        when(activatePlanUseCase.execute(eq(EXPECTED_ID))).thenReturn(outputOf(true));

        final var actual = mvc.perform(put(PLANS_PATH + "/" + EXPECTED_ID + "/activate"));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.price").value(49.90));
    }

    @Test
    void givenAnExistingPlan_whenDeactivates_thenAnswersOkSayingItIsInactive() throws Exception {
        when(deactivatePlanUseCase.execute(eq(EXPECTED_ID))).thenReturn(outputOf(false));

        final var actual = mvc.perform(put(PLANS_PATH + "/" + EXPECTED_ID + "/deactivate"));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void givenAMissingPlan_whenActivates_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.with(Plan.class, PlanId.from(EXPECTED_ID)))
                .when(activatePlanUseCase)
                .execute(any());

        final var actual = mvc.perform(put(PLANS_PATH + "/" + EXPECTED_ID + "/activate"));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenAnExistingPlan_whenGetsItById_thenAnswersOkWithTheWholePlan() throws Exception {
        when(getPlanByIdUseCase.execute(eq(EXPECTED_ID))).thenReturn(outputOf(true));

        final var actual = mvc.perform(get(PLANS_PATH + "/" + EXPECTED_ID));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXPECTED_ID))
                .andExpect(jsonPath("$.name").value("Plus"))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void givenAnInactivePlan_whenGetsItById_thenItIsStillAnswered() throws Exception {
        when(getPlanByIdUseCase.execute(eq(EXPECTED_ID))).thenReturn(outputOf(false));

        final var actual = mvc.perform(get(PLANS_PATH + "/" + EXPECTED_ID));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void givenAMissingPlan_whenGetsItById_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.with(Plan.class, PlanId.from(EXPECTED_ID)))
                .when(getPlanByIdUseCase)
                .execute(any());

        final var actual = mvc.perform(get(PLANS_PATH + "/" + EXPECTED_ID));

        actual.andExpect(status().isNotFound());
    }

    private static PlanOutput outputOf(final boolean active) {
        return new PlanOutput(
                EXPECTED_ID,
                "Plus",
                "Quatro telas",
                active,
                "BRL",
                new BigDecimal("49.90"),
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z"));
    }
}
