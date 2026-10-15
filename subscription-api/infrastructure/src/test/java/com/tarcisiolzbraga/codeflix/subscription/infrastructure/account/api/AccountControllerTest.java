package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.api;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.account.billing.ChangeBillingAddressUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.get.GetMyAccountUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ControllerTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Importa a configuração de segurança, diferente do PlanControllerTest: as rotas de
// autoatendimento recebem o `Principal`, e quem o põe na requisição é a cadeia de filtros do Spring
// Security. Sem ela o parâmetro chega nulo — descoberto do jeito difícil, com sete testes vermelhos
// e NullPointerException.
//
// O efeito colateral é bom: o teste passa a provar também que um token de assinante alcança estas
// rotas, e que as duas do cadastro seguem abertas.
@ControllerTest(controllers = AccountController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "keycloak.issuer-uri=http://localhost:8081/realms/codeflix")
class AccountControllerTest {

    private static final String SIGN_UP_PATH = "/accounts/sign-up";
    private static final String CONFIRM_PATH = "/accounts/sign-up/confirm";
    private static final String ME_PATH = "/accounts/me";
    private static final String BILLING_PATH = "/accounts/me/billing-address";
    private static final String EXPECTED_ID = "11111111-1111-1111-1111-111111111111";
    private static final String PROVIDER_ID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
    private static final String EMAIL = "assinante@codeflix.com.br";
    private static final String ROLE_ACCOUNT = "ROLE_CODEFLIX_ACCOUNT";
    private static final String TOKEN_BODY = "{\"token\":\"um-token\"}";
    private static final String SIGN_UP_BODY =
            """
            {"firstname":"Tarcisio","lastname":"Braga","email":"assinante@codeflix.com.br",\
            "password":"uma-senha-boa","documentNumber":"11144477735","documentType":"cpf"}""";
    private static final String ADDRESS_BODY =
            """
            {"zipcode":"01310930","number":"1578","complement":"apto 42","country":"BR"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SignUpUseCase signUpUseCase;

    @MockitoBean
    private ConfirmSignUpUseCase confirmSignUpUseCase;

    @MockitoBean
    private GetMyAccountUseCase getMyAccountUseCase;

    @MockitoBean
    private ChangeBillingAddressUseCase changeBillingAddressUseCase;

    @Test
    void givenAValidBody_whenSignsUp_thenAnswersAcceptedWithTheGenericMessage() throws Exception {
        final var actual =
                mvc.perform(post(SIGN_UP_PATH).contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY));

        actual.andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("a validation email is on its way"));
    }

    // argThat, e não any(): sem isto renomear um campo do request faria o valor chegar nulo.
    @Test
    void givenAValidBody_whenSignsUp_thenTheCommandCarriesTheValuesFromTheBody() throws Exception {
        mvc.perform(post(SIGN_UP_PATH).contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY));

        verify(signUpUseCase).execute(argThat(command -> "Tarcisio".equals(command.firstname())
                && "Braga".equals(command.lastname())
                && EMAIL.equals(command.email())
                && "uma-senha-boa".equals(command.password())
                && "11144477735".equals(command.documentNumber())
                && "cpf".equals(command.documentType())));
    }

    @Test
    void givenDataTheDomainRefuses_whenSignsUp_thenAnswersUnprocessable() throws Exception {
        doThrow(DomainException.with("'cpf' is invalid")).when(signUpUseCase).execute(any());

        final var actual =
                mvc.perform(post(SIGN_UP_PATH).contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("'cpf' is invalid"));
    }

    @Test
    void givenAValidToken_whenConfirms_thenAnswersOkWithTheAccountId() throws Exception {
        when(confirmSignUpUseCase.execute(any())).thenReturn(new ConfirmSignUpOutput(EXPECTED_ID));

        final var actual =
                mvc.perform(post(CONFIRM_PATH).contentType(MediaType.APPLICATION_JSON).content(TOKEN_BODY));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value(EXPECTED_ID));
    }

    @Test
    void givenAValidToken_whenConfirms_thenTheCommandCarriesTheTokenFromTheBody() throws Exception {
        when(confirmSignUpUseCase.execute(any())).thenReturn(new ConfirmSignUpOutput(EXPECTED_ID));

        mvc.perform(post(CONFIRM_PATH).contentType(MediaType.APPLICATION_JSON).content(TOKEN_BODY));

        verify(confirmSignUpUseCase).execute(argThat(command -> "um-token".equals(command.token())));
    }

    @Test
    void givenAnUnknownToken_whenConfirms_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.signUpLink()).when(confirmSignUpUseCase).execute(any());

        final var actual =
                mvc.perform(post(CONFIRM_PATH).contentType(MediaType.APPLICATION_JSON).content(TOKEN_BODY));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenAnEmailOwnedBySomeoneElse_whenConfirms_thenAnswersConflict() throws Exception {
        doThrow(ConflictException.emailAlreadyTaken(EMAIL)).when(confirmSignUpUseCase).execute(any());

        final var actual =
                mvc.perform(post(CONFIRM_PATH).contentType(MediaType.APPLICATION_JSON).content(TOKEN_BODY));

        actual.andExpect(status().isConflict());
    }

    @Test
    void givenAnExpiredLink_whenConfirms_thenAnswersUnprocessableWithTheReason() throws Exception {
        doThrow(DomainException.with("the sign-up link expired; sign up again"))
                .when(confirmSignUpUseCase)
                .execute(any());

        final var actual =
                mvc.perform(post(CONFIRM_PATH).contentType(MediaType.APPLICATION_JSON).content(TOKEN_BODY));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("the sign-up link expired; sign up again"));
    }

    // O `sub` do token é o que identifica quem chama, e é ele que tem de chegar ao caso de uso.
    @Test
    void givenATokenWithASubject_whenAsksForItsOwnAccount_thenTheUseCaseGetsThatSubject() throws Exception {
        when(getMyAccountUseCase.execute(any())).thenReturn(anAccountOutput(null));

        mvc.perform(get(ME_PATH).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID))));

        verify(getMyAccountUseCase).execute(PROVIDER_ID);
    }

    @Test
    void givenAnAccountWithoutBillingAddress_whenAsksForIt_thenTheFieldComesNull() throws Exception {
        when(getMyAccountUseCase.execute(any())).thenReturn(anAccountOutput(null));

        final var actual = mvc.perform(get(ME_PATH).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID))));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.billingAddress").value(nullValue()));
    }

    @Test
    void givenAnAccountWithBillingAddress_whenAsksForIt_thenTheAddressComesAlong() throws Exception {
        when(getMyAccountUseCase.execute(any())).thenReturn(anAccountOutput(anAddress()));

        final var actual = mvc.perform(get(ME_PATH).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID))));

        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.billingAddress.zipcode").value("01310930"))
                .andExpect(jsonPath("$.billingAddress.country").value("BR"));
    }

    @Test
    void givenSomeoneWithoutAnAccount_whenAsksForIt_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.callerAccount()).when(getMyAccountUseCase).execute(any());

        final var actual = mvc.perform(get(ME_PATH).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID))));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenAnAddressBody_whenChangesIt_thenTheCommandCarriesTheSubjectAndTheBody() throws Exception {
        when(changeBillingAddressUseCase.execute(any())).thenReturn(anAccountOutput(anAddress()));

        mvc.perform(put(BILLING_PATH)
                .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADDRESS_BODY));

        verify(changeBillingAddressUseCase).execute(argThat(command -> PROVIDER_ID.equals(command.userId())
                && "01310930".equals(command.zipcode())
                && "1578".equals(command.number())
                && "apto 42".equals(command.complement())
                && "BR".equals(command.country())));
    }

    @Test
    void givenAnAddressBody_whenChangesIt_thenAnswersOkWithTheAccountAlreadyUpdated() throws Exception {
        when(changeBillingAddressUseCase.execute(any())).thenReturn(anAccountOutput(anAddress()));

        final var actual = mvc.perform(put(BILLING_PATH)
                .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADDRESS_BODY));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.billingAddress.number").value("1578"));
    }

    @Test
    void givenAnIncompleteAddress_whenChangesIt_thenAnswersUnprocessable() throws Exception {
        doThrow(DomainException.with("'number' should not be empty"))
                .when(changeBillingAddressUseCase)
                .execute(any());

        final var actual = mvc.perform(put(BILLING_PATH)
                .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_ACCOUNT)).jwt(it -> it.subject(PROVIDER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADDRESS_BODY));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("'number' should not be empty"));
    }

    private static AccountOutput anAccountOutput(final AccountOutput.AddressOutput address) {
        return new AccountOutput(EXPECTED_ID, EMAIL, "Tarcisio", "Braga", "11144477735", "cpf", address);
    }

    private static AccountOutput.AddressOutput anAddress() {
        return new AccountOutput.AddressOutput("01310930", "1578", "apto 42", "BR");
    }
}
