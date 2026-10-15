package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Entra no contexto de toda jornada pelo @E2ETest, e não como classe aninhada no teste: com
// `classes` fixo no @SpringBootTest, o Spring não procura configuração aninhada na classe de teste
// — descoberto com treze testes vermelhos e nenhum bean de relógio.
//
// Nome de bean diferente do de produção, e @Primary: não é sobrescrita, são dois relógios no
// contexto, e este é o que os casos de uso recebem.
@TestConfiguration(proxyBeanMethods = false)
public class JourneyClockConfiguration {

    @Bean
    @Primary
    AdvanceableClock journeyClock() {
        return AdvanceableClock.overTheSystemClock();
    }
}
