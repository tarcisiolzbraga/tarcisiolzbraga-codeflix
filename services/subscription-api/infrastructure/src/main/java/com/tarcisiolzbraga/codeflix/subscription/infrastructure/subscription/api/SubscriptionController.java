package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.api;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.cancel.CancelSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.get.GetMySubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe.SubscribeCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe.SubscribeUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.ChargeRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.ChargeResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.SubscribeRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models.SubscriptionResponse;
import java.net.URI;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController implements SubscriptionAPI {

    private final SubscribeUseCase subscribeUseCase;
    private final GetMySubscriptionUseCase getMySubscriptionUseCase;
    private final ChargeSubscriptionUseCase chargeSubscriptionUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;

    public SubscriptionController(
            final SubscribeUseCase subscribeUseCase,
            final GetMySubscriptionUseCase getMySubscriptionUseCase,
            final ChargeSubscriptionUseCase chargeSubscriptionUseCase,
            final CancelSubscriptionUseCase cancelSubscriptionUseCase) {
        this.subscribeUseCase = Objects.requireNonNull(subscribeUseCase);
        this.getMySubscriptionUseCase = Objects.requireNonNull(getMySubscriptionUseCase);
        this.chargeSubscriptionUseCase = Objects.requireNonNull(chargeSubscriptionUseCase);
        this.cancelSubscriptionUseCase = Objects.requireNonNull(cancelSubscriptionUseCase);
    }

    // O Location aponta para /subscriptions/me, e não para /subscriptions/{id}: é a URL que existe,
    // porque é por ela que a pessoa lê a própria assinatura. O id vem no corpo.
    @Override
    public ResponseEntity<SubscriptionResponse> subscribe(final Principal caller, final SubscribeRequest request) {
        final var command = new SubscribeCommand(caller.getName(), request.planId());
        final var response = SubscriptionResponse.from(this.subscribeUseCase.execute(command));
        return ResponseEntity.created(URI.create("/subscriptions/me")).body(response);
    }

    @Override
    public SubscriptionResponse mySubscription(final Principal caller) {
        return SubscriptionResponse.from(this.getMySubscriptionUseCase.execute(caller.getName()));
    }

    @Override
    public ChargeResponse charge(final Principal caller, final ChargeRequest request) {
        final var command = new ChargeSubscriptionCommand(caller.getName(), request.creditCardToken());
        return ChargeResponse.from(this.chargeSubscriptionUseCase.execute(command));
    }

    @Override
    public SubscriptionResponse cancel(final Principal caller) {
        return SubscriptionResponse.from(this.cancelSubscriptionUseCase.execute(caller.getName()));
    }
}
