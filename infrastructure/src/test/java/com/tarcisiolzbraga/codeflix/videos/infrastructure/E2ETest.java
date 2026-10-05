package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

// Jornadas do cliente sobre HTTP de verdade: sobe o Tomcat numa porta aleatória, o Elasticsearch e o
// Kafka em container. Diferente do @IntegrationTest, aqui nada é simulado — nem o servidor, nem o
// armazenamento, nem o broker.
//
// O Kafka entra porque o contexto sobe o listener do CDC: sem broker ele ficaria tentando conectar
// e enchendo o log, e o teste dependeria de uma falha tolerada em vez de um ambiente completo.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("e2eTest")
@SpringBootTest(classes = Main.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({ElasticsearchContainerConfiguration.class, KafkaContainerConfiguration.class})
@ExtendWith(ElasticsearchCleanUpExtension.class)
public @interface E2ETest {
}
