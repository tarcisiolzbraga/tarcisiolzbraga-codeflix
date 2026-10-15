package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionGateway {

    // A assinatura mais recente da conta. Uma conta assina uma vez por vez, mas pode reassinar
    // depois de cancelar — então o que interessa é a última, e não "a" assinatura.
    Optional<Subscription> latestSubscriptionOfAccount(AccountId anAccountId);

    Subscription save(Subscription aSubscription);

    // As assinaturas cobráveis vencidas até a data, em lote, para a varredura automática.
    //
    // Com limite e deslocamento obrigatórios: coleção sem limite vinda do banco é proibida no
    // monorepo, e aqui a razão é concreta — a varredura roda sobre a base inteira e carregar tudo
    // numa lista cresce com o número de assinantes.
    //
    // Canceladas ficam de fora na consulta, e não no chamador: elas nunca voltam a ser cobráveis, e
    // deixá-las vir só para serem descartadas faria a página render menos trabalho útil a cada
    // execução, até a varredura parar de alcançar o fim.
    List<Subscription> chargeableSubscriptionsDueBy(LocalDate aDate, int limit, int offset);
}
