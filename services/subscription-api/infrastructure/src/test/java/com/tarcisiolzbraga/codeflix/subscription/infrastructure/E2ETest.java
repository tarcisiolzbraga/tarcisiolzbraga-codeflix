package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

// Jornadas sobre HTTP de verdade: Tomcat em porta aleatória e MySQL em container. Diferente da fatia
// web, aqui nada é simulado — e é só aqui que o container de servlet existe, o que importa para tudo
// o que depende de despacho de erro, filtro e codificação de resposta.
//
// **O teto de requisição fica alto aqui, e o limitador segue ligado.** Não é o limite desligado: é
// o limite fora do caminho de quem testa outra coisa. Todas as jornadas dividem um contexto, logo
// dividem os mesmos baldes, e com o teto de produção (5 cadastros em 10 minutos) o sexto teste da
// suíte levava 429 — 19 testes caíram assim, e qual deles caía dependia da ordem de execução.
//
// Quem exercita o limitador é o RateLimitE2ETest, que declara os próprios tetos — apertados — e com
// isso sobe num contexto à parte. Então o comportamento tem teste, e as jornadas não pagam por ele.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("e2eTest")
@SpringBootTest(classes = Main.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "rate-limit.sign-up.capacity=1000",
            "rate-limit.confirm.capacity=1000",
            "rate-limit.general.capacity=10000"
        })
@Import({MySQLContainerConfiguration.class, KeycloakContainerConfiguration.class,
        RabbitMQContainerConfiguration.class,
        KafkaContainerConfiguration.class, JourneyClockConfiguration.class})
public @interface E2ETest {}
