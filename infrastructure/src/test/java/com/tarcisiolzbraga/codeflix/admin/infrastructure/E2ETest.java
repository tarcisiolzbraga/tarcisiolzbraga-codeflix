package com.tarcisiolzbraga.codeflix.admin.infrastructure;

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

// Jornadas do usuário sobre HTTP de verdade: sobe o Tomcat numa porta aleatória e um MySQL em
// container. Diferente do @IntegrationTest, aqui nada é simulado — nem o servidor, nem o banco.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("e2eTest")
@SpringBootTest(classes = Main.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({MySQLContainerConfiguration.class, GarageContainerConfiguration.class,
        RabbitMQContainerConfiguration.class})
@ExtendWith({MySQLCleanUpExtension.class, RabbitCleanUpExtension.class})
public @interface E2ETest {
}
