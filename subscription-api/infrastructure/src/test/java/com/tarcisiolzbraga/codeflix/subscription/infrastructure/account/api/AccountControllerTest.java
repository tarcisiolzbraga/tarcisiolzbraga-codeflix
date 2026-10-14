package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = AccountController.class)
class AccountControllerTest {

    private static final String SIGN_UP_PATH = "/accounts/sign-up";
    private static final String CONFIRM_PATH = "/accounts/sign-up/confirm";
    private static final String EXPECTED_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SIGN_UP_BODY =
            """
            {"firstname":"Tarcisio","lastname":"Braga","email":"assinante@codeflix.com.br",\
            "password":"uma-senha-boa","documentNumber":"11144477735","documentType":"cpf"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SignUpUseCase signUpUseCase;

    @MockitoBean
    private ConfirmSignUpUseCase confirmSignUpUseCase;

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
                && "assinante@codeflix.com.br".equals(command.email())
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

        final var actual = mvc.perform(post(CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"um-token\"}"));

        actual.andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value(EXPECTED_ID));
    }

    @Test
    void givenAValidToken_whenConfirms_thenTheCommandCarriesTheTokenFromTheBody() throws Exception {
        when(confirmSignUpUseCase.execute(any())).thenReturn(new ConfirmSignUpOutput(EXPECTED_ID));

        mvc.perform(post(CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"um-token\"}"));

        verify(confirmSignUpUseCase).execute(argThat(command -> "um-token".equals(command.token())));
    }

    @Test
    void givenAnUnknownToken_whenConfirms_thenAnswersNotFound() throws Exception {
        doThrow(NotFoundException.signUpLink()).when(confirmSignUpUseCase).execute(any());

        final var actual = mvc.perform(post(CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"um-token\"}"));

        actual.andExpect(status().isNotFound());
    }

    @Test
    void givenAnEmailOwnedBySomeoneElse_whenConfirms_thenAnswersConflict() throws Exception {
        doThrow(ConflictException.emailAlreadyTaken("assinante@codeflix.com.br"))
                .when(confirmSignUpUseCase)
                .execute(any());

        final var actual = mvc.perform(post(CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"um-token\"}"));

        actual.andExpect(status().isConflict());
    }

    @Test
    void givenAnExpiredLink_whenConfirms_thenAnswersUnprocessableWithTheReason() throws Exception {
        doThrow(DomainException.with("the sign-up link expired; sign up again"))
                .when(confirmSignUpUseCase)
                .execute(any());

        final var actual = mvc.perform(post(CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"um-token\"}"));

        actual.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("the sign-up link expired; sign up again"));
    }
}
