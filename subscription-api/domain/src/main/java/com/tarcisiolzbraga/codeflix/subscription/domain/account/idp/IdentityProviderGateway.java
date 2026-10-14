package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import java.util.Optional;

// O que o cadastro precisa do provedor de identidade, em linguagem de negócio.
//
// `addToSubscribers` em vez do `addUserToGroup(UserId, GroupId)` da referência: a intenção do
// domínio é "esta pessoa é assinante", e grupo é como o Keycloak concede isso. Com o id do grupo na
// assinatura do método, um conceito do provedor atravessaria o domínio inteiro — e, pior, o
// id muda a cada reimportação do realm, que é descartável aqui.
public interface IdentityProviderGateway {

    UserId create(NewUser aNewUser);

    Optional<User> userOfEmail(Email anEmail);

    // Chamado na confirmação do cadastro: até ali o provedor tem a pessoa com o e-mail não conferido.
    void markEmailVerified(UserId anUserId);

    void addToSubscribers(UserId anUserId);
}
