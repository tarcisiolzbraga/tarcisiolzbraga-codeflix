package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O relógio do serviço, em bean para o teste poder adiantá-lo.
//
// UTC e não o fuso da máquina: o que depende de tempo aqui é a validade da URL assinada da mídia, e
// ela é comparada contra um instante que o cliente recebeu — fuso da máquina faria a validade
// depender de onde o serviço roda.
//
// A subscription-api tem o seu, pelo mesmo motivo e com a mesma forma.
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
