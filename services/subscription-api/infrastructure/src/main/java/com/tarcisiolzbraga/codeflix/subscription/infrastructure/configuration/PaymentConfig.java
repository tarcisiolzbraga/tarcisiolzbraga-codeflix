package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.payment.FakePaymentGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O provedor de pagamento. **Este serviço nunca cobra ninguém**: o provedor é de mentira, em todos
// os perfis, e é assim que fica.
//
// A primeira versão disto tinha `@Profile("!production")`, para produção não subir sem um provedor
// de verdade — "não subir é melhor que subir aprovando toda cobrança em silêncio". O raciocínio
// valia enquanto um provedor de verdade estava no caminho. Decidido que este é um projeto de prova
// de conceito e que não haverá cobrança real, o guard deixou de proteger algo e passou a ser
// armadilha: o perfil `production` nunca subiria, e quem tentasse levaria uma falha de bean
// faltando, sem explicação nenhuma.
//
// O que substitui o guard é dizer a verdade em três lugares: aqui, no aviso que o
// FakePaymentGateway escreve no log a cada cobrança, e no README. Se um dia houver provedor de
// verdade, ele entra como outro @Bean e este volta a ser condicional.
@Configuration(proxyBeanMethods = false)
public class PaymentConfig {

    @Bean
    PaymentGateway paymentGateway() {
        return new FakePaymentGateway();
    }
}
