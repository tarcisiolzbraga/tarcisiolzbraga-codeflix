package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import java.util.Optional;

public interface AccountGateway {

    Optional<Account> accountOfId(AccountId anId);

    // Usada pelo cadastro: se já existe conta para o e-mail, não há o que confirmar, e a resposta
    // continua sendo a genérica — não se revela que a conta existe.
    Optional<Account> accountOfEmail(Email anEmail);

    // Devolve o próprio agregado recebido. A versão dele continua sendo a que foi usada no lock, e
    // não a que ficou gravada: para salvar de novo é preciso ler a conta outra vez.
    Account save(Account anAccount);
}
