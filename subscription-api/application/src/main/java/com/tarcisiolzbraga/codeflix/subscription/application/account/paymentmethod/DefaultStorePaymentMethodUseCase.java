package com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod;

import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.account.CallerAccount;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import java.util.Objects;

// Guarda o meio de pagamento da conta, e é o que torna a cobrança automática possível: sem ele, só
// se cobra quem está presente com o cartão na mão.
//
// O token de uso único **não é persistido**. Ele vai ao provedor, que devolve uma referência
// durável, e é só ela que chega ao banco — um token de uso único guardado seria dado de cartão
// parado aqui, com prazo de validade e sem utilidade.
public class DefaultStorePaymentMethodUseCase extends StorePaymentMethodUseCase {

    private final AccountGateway accountGateway;
    private final PaymentGateway payments;

    public DefaultStorePaymentMethodUseCase(
            final AccountGateway accountGateway, final PaymentGateway payments) {
        this.accountGateway = Objects.requireNonNull(accountGateway, "'accountGateway' should not be null");
        this.payments = Objects.requireNonNull(payments, "'payments' should not be null");
    }

    @Override
    public AccountOutput execute(final StorePaymentMethodCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var account = CallerAccount.of(this.accountGateway, command.userId());

        if (command.creditCardToken() == null || command.creditCardToken().isBlank()) {
            throw DomainException.with("'creditCardToken' should not be empty");
        }

        // A conta só é tocada depois de o provedor confirmar que guardou: na ordem inversa, uma
        // falha do provedor deixaria no banco uma referência que não existe do lado dele.
        final var paymentMethod = this.payments.storeCard(command.creditCardToken());
        account.execute(new AccountCommand.ChangePaymentMethod(paymentMethod));

        return AccountOutput.from(this.accountGateway.save(account));
    }
}
