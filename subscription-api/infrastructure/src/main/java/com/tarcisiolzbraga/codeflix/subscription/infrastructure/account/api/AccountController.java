package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.api;

import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpResponse;
import java.util.Objects;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController implements AccountAPI {

    private final SignUpUseCase signUpUseCase;
    private final ConfirmSignUpUseCase confirmSignUpUseCase;

    public AccountController(
            final SignUpUseCase signUpUseCase, final ConfirmSignUpUseCase confirmSignUpUseCase) {
        this.signUpUseCase = Objects.requireNonNull(signUpUseCase);
        this.confirmSignUpUseCase = Objects.requireNonNull(confirmSignUpUseCase);
    }

    // A resposta é montada aqui e não vem do caso de uso, que não devolve nada: assim o controller
    // não tem como responder diferente por acidente em algum dos casos.
    @Override
    public SignUpResponse signUp(final SignUpRequest request) {
        this.signUpUseCase.execute(SignUpCommand.with(
                request.firstname(),
                request.lastname(),
                request.email(),
                request.password(),
                request.documentNumber(),
                request.documentType()));
        return SignUpResponse.accepted();
    }

    @Override
    public ConfirmSignUpResponse confirmSignUp(final ConfirmSignUpRequest request) {
        return ConfirmSignUpResponse.from(
                this.confirmSignUpUseCase.execute(new ConfirmSignUpCommand(request.token())));
    }
}
