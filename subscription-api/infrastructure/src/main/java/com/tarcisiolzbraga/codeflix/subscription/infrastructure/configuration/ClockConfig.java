package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O relógio do serviço, em configuração própria.
//
// Morava no KeycloakAdminConfig, de quando o único a consultá-lo era a renovação do token de client.
// Hoje são sete: o vencimento da assinatura, a tolerância da cobrança, a varredura das vencidas, o
// prazo do link de cadastro. Relógio não é assunto do Keycloak, e deixá-lo lá fazia quem procurasse
// por ele não achar.
//
// Em UTC, e não no fuso do sistema: é o mesmo fuso em que a assinatura guarda o vencimento, e um
// relógio em fuso local faria o dia da virada depender de onde o processo roda.
//
// **É este bean que as jornadas de teste substituem** por um relógio adiantável, para a cobrança
// poder esperar o vencimento chegar sem esperar um mês.
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
