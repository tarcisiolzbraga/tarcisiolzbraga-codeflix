package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.cancel.CancelSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.get.GetMySubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe.SubscribeUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionId;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ControllerTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.SecurityConfig;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Importa a configuração de segurança pelo mesmo motivo do AccountControllerTest: toda rota daqui
// recebe o `Principal`, e quem o põe na requisição é a cadeia de filtros do Spring Security.
@ControllerTest(controllers = SubscriptionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "keycloak.issuer-uri=http://localhost:8081/realms/codeflix")
class SubscriptionControllerTest {

    private static final String SUBSCRIPTIONS_PATH = "/subscriptions";
    private static final String ME_PATH = "/subscriptions/me";
    private static final String CHARGE_PATH = "/subscriptions/me/charge";
    private static final String CANCEL_PATH = "/subscriptions/me/cancel";
    private static final String ID = "11111111-1111-1111-1111-111111111111";
    private static final String PLAN_ID = "22222222-2222-2222-2222-222222222222";
    private static final String PROVIDER_ID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
    private static final String ROLE_ACCOUNT = "ROLE_CODEFLIX_ACCOUNT";
    private static final String SUBSCRIBE_BODY = "{\"planId\":\"22222222-2222-2222-2222-222222222222\"}";
    private static final String CHARGE_BODY = "{\"creditCardToken\":\"tok_visa_4242\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SubscribeUseCase subscribeUseCase;

    @MockitoBean
    private GetMySubscriptionUseCase getMySubscriptionUseCase;

    @MockitoBean
    private ChargeSubscriptionUseCase chargeSubscriptionUseCase;

    @MockitoBean
    private CancelSubscriptionUseCase cancelSubscriptionUseCase;

    @Test
    void givenAPlan_whenSubscribes_thenAnswersCreatedWithTheSubscription() throws Exception {
        when(subscribeUseCase.execute(any())).thenReturn(anOutput("trailing"));

        final var actual = mvc.perform(asSubscriber(post(SUBSCRIPTIONS_PATH)).content(SUBSCRIBE_BODY));

        actual.andExpect(status().isCreated())
                .andExpect(header().string("Location", ME_PATH))
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.planId").value(PLAN_ID))
                .andExpect(jsonPath("$.status").value("trailing"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-14"));
    }

    // argThat, e não any(): sem isto trocar a ordem dos dois campos do comando passaria batido, e
    // o id de quem assina iria para o lugar do plano.
    @Test
    void givenAPlan_whenSubscribes_thenTheCommandCarriesTheSubjectAndThePlan() throws Exception {
        when(subscribeUseCase.execute(any())).thenReturn(anOutput("trailing"));

        mvc.perform(asSubscriber(post(SUBSCRIPTIONS_PATH)).content(SUBSCRIBE_BODY));

        verify(subscribeUseCase)
                .execute(argThat(command -> PROVIDER_ID.equals(command.userId()) && PLAN_ID.equals(command.planId())));
    }

    @Test
    void givenSomeoneWithoutAnAccount_whenSubscribes_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.callerAccount()).when(subscribeUseCase).execute(any());

        final var actual = mvc.perform(asSubscriber(post(SUBSCRIPTIONS_PATH)).content(SUBSCRIBE_BODY));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenSomeoneAlreadySubscribed_whenSubscribes_thenAnswersConflict() throws Exception {
        doThrow(ConflictException.alreadySubscribed()).when(subscribeUseCase).execute(any());

        final var actual = mvc.perform(asSubscriber(post(SUBSCRIPTIONS_PATH)).content(SUBSCRIBE_BODY));

        actual.andExpect(status().isConflict());
    }

    @Test
    void givenAPlanOutOfSale_whenSubscribes_thenAnswersUnprocessableWithTheReason() throws Exception {
        doThrow(DomainException.with("the plan is not active")).when(subscribeUseCase).execute(any());

        final var actual = mvc.perform(asSubscriber(post(SUBSCRIPTIONS_PATH)).content(SUBSCRIBE_BODY));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("the plan is not active"));
    }

    @Test
    void givenATokenWithASubject_whenAsksForItsOwnSubscription_thenTheUseCaseGetsThatSubject() throws Exception {
        when(getMySubscriptionUseCase.execute(any())).thenReturn(anOutput("active"));

        mvc.perform(asSubscriber(get(ME_PATH)));

        verify(getMySubscriptionUseCase).execute(PROVIDER_ID);
    }

    @Test
    void givenASubscriptionNeverRenewed_whenAsksForIt_thenTheFieldComesNull() throws Exception {
        when(getMySubscriptionUseCase.execute(any())).thenReturn(anOutput("trailing"));

        final var actual = mvc.perform(asSubscriber(get(ME_PATH)));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("trailing"))
                .andExpect(jsonPath("$.lastRenewDate").value(nullValue()));
    }

    @Test
    void givenSomeoneWhoNeverSubscribed_whenAsksForIt_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.callerSubscription()).when(getMySubscriptionUseCase).execute(any());

        final var actual = mvc.perform(asSubscriber(get(ME_PATH)));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenACardToken_whenCharges_thenTheCommandCarriesTheSubjectAndTheToken() throws Exception {
        when(chargeSubscriptionUseCase.execute(any())).thenReturn(aSucceededCharge());

        mvc.perform(asSubscriber(post(CHARGE_PATH)).content(CHARGE_BODY));

        verify(chargeSubscriptionUseCase).execute(argThat(command -> PROVIDER_ID.equals(command.userId())
                && "tok_visa_4242".equals(command.creditCardToken())));
    }

    @Test
    void givenAChargeThatPassed_whenCharges_thenAnswersOkWithTheTransaction() throws Exception {
        when(chargeSubscriptionUseCase.execute(any())).thenReturn(aSucceededCharge());

        final var actual = mvc.perform(asSubscriber(post(CHARGE_PATH)).content(CHARGE_BODY));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.charged").value(true))
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.subscription.status").value("active"));
    }

    // Recusa não é erro da requisição: o serviço fez o que devia, e quem recusou foi o provedor. O
    // resultado vai no corpo com o motivo, que é o que o cliente mostra para a pessoa resolver.
    @Test
    void givenAChargeThatWasRefused_whenCharges_thenStillAnswersOkWithTheReason() throws Exception {
        when(chargeSubscriptionUseCase.execute(any()))
                .thenReturn(new ChargeSubscriptionOutput(
                        anOutput("incomplete"), false, "tx-1", "the card was refused"));

        final var actual = mvc.perform(asSubscriber(post(CHARGE_PATH)).content(CHARGE_BODY));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.charged").value(false))
                .andExpect(jsonPath("$.reason").value("the card was refused"))
                .andExpect(jsonPath("$.subscription.status").value("incomplete"));
    }

    @Test
    void givenAnAccountWithoutBillingAddress_whenCharges_thenAnswersUnprocessableWithTheReason() throws Exception {
        doThrow(DomainException.with("the account has no billing address; set one before charging"))
                .when(chargeSubscriptionUseCase)
                .execute(any());

        final var actual = mvc.perform(asSubscriber(post(CHARGE_PATH)).content(CHARGE_BODY));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("the account has no billing address; set one before charging"));
    }

    @Test
    void givenATokenWithASubject_whenCancels_thenTheUseCaseGetsThatSubjectAndAnswersOk() throws Exception {
        when(cancelSubscriptionUseCase.execute(any())).thenReturn(anOutput("canceled"));

        final var actual = mvc.perform(asSubscriber(put(CANCEL_PATH)));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("canceled"));
        verify(cancelSubscriptionUseCase).execute(PROVIDER_ID);
    }

    @Test
    void givenSomeoneWhoNeverSubscribed_whenCancels_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.callerSubscription()).when(cancelSubscriptionUseCase).execute(any());

        final var actual = mvc.perform(asSubscriber(put(CANCEL_PATH)));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenASubscriptionChangedMeanwhile_whenCancels_thenAnswersConflict() throws Exception {
        doThrow(ConflictException.staleVersion(Subscription.class, SubscriptionId.from(ID)))
                .when(cancelSubscriptionUseCase)
                .execute(any());

        final var actual = mvc.perform(asSubscriber(put(CANCEL_PATH)));

        actual.andExpect(status().isConflict());
    }

    private static MockHttpServletRequestBuilder asSubscriber(final MockHttpServletRequestBuilder request) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT))
                        .jwt(it -> it.subject(PROVIDER_ID)))
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static ChargeSubscriptionOutput aSucceededCharge() {
        return new ChargeSubscriptionOutput(anOutput("active"), true, "tx-1", null);
    }

    private static SubscriptionOutput anOutput(final String status) {
        return new SubscriptionOutput(
                ID,
                PLAN_ID,
                status,
                LocalDate.parse("2026-11-14"),
                "trailing".equals(status) ? null : Instant.parse("2026-10-14T12:00:00Z"),
                Instant.parse("2026-10-14T12:00:00Z"));
    }
}
