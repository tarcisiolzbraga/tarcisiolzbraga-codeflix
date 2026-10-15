package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.payment.FakePaymentGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// O provedor de pagamento. Hoje só existe o de mentira, e ele **não sobe em produção**.
//
// Isso é escolha, não esquecimento: sem um provedor de verdade configurado, produção não sobe — e
// não subir é melhor que subir aprovando toda cobrança em silêncio. O dia em que houver provedor de
// verdade, ele entra como outro @Bean, com o perfil invertido.
@Configuration(proxyBeanMethods = false)
public class PaymentConfig {

    @Bean
    @Profile("!production")
    PaymentGateway paymentGateway() {
        return new FakePaymentGateway();
    }
}
