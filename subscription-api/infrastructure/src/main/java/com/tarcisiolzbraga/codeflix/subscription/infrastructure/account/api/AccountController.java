package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.api;

import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.account.billing.ChangeBillingAddressCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.account.billing.ChangeBillingAddressUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.get.GetMyAccountUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.AccountResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.BillingAddressRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.ConfirmSignUpResponse;
import com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod.StorePaymentMethodCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod.StorePaymentMethodUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.PaymentMethodRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.SignUpResponse;
import java.security.Principal;
import java.util.Objects;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController implements AccountAPI {

    private final SignUpUseCase signUpUseCase;
    private final ConfirmSignUpUseCase confirmSignUpUseCase;
    private final GetMyAccountUseCase getMyAccountUseCase;
    private final ChangeBillingAddressUseCase changeBillingAddressUseCase;
    private final StorePaymentMethodUseCase storePaymentMethodUseCase;

    public AccountController(
            final SignUpUseCase signUpUseCase,
            final ConfirmSignUpUseCase confirmSignUpUseCase,
            final GetMyAccountUseCase getMyAccountUseCase,
            final ChangeBillingAddressUseCase changeBillingAddressUseCase,
            final StorePaymentMethodUseCase storePaymentMethodUseCase) {
        this.signUpUseCase = Objects.requireNonNull(signUpUseCase);
        this.confirmSignUpUseCase = Objects.requireNonNull(confirmSignUpUseCase);
        this.getMyAccountUseCase = Objects.requireNonNull(getMyAccountUseCase);
        this.changeBillingAddressUseCase = Objects.requireNonNull(changeBillingAddressUseCase);
        this.storePaymentMethodUseCase = Objects.requireNonNull(storePaymentMethodUseCase);
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

    // O nome do Principal é o `sub` do token: o KeycloakJwtConverter o põe ali, e ele é o id da
    // pessoa no provedor de identidade — o mesmo que a conta guarda.
    @Override
    public AccountResponse myAccount(final Principal caller) {
        return AccountResponse.from(this.getMyAccountUseCase.execute(caller.getName()));
    }

    @Override
    public AccountResponse changeBillingAddress(final Principal caller, final BillingAddressRequest request) {
        final var command = ChangeBillingAddressCommand.with(
                caller.getName(), request.zipcode(), request.number(), request.complement(), request.country());
        return AccountResponse.from(this.changeBillingAddressUseCase.execute(command));
    }

    @Override
    public AccountResponse storePaymentMethod(final Principal caller, final PaymentMethodRequest request) {
        final var command = new StorePaymentMethodCommand(caller.getName(), request.creditCardToken());
        return AccountResponse.from(this.storePaymentMethodUseCase.execute(command));
    }
}
