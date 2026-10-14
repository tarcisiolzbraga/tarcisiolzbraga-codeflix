package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import java.util.Optional;

// Só o que algum caso de uso desta fatia usa. As buscas por e-mail e por id do provedor entram com
// o cadastro, na fatia seguinte.
public interface AccountGateway {

    Optional<Account> accountOfId(AccountId anId);

    // Devolve o próprio agregado recebido. A versão dele continua sendo a que foi usada no lock, e
    // não a que ficou gravada: para salvar de novo é preciso ler a conta outra vez.
    Account save(Account anAccount);
}
