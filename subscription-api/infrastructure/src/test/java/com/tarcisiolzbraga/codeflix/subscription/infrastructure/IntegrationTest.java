package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// O serviço com um MySQL e um Keycloak de verdade, sem servidor web. Os dois containers são
// estáticos e sobem uma vez por execução, então quem não usa o Keycloak não paga por ele duas vezes. A tag separa quem precisa de Docker: a task
// unitTests exclui esta e a do e2e.
//
// classes explícito porque os IT de caso de uso ficam no pacote da application, de onde a busca por
// @SpringBootConfiguration subindo pelos pacotes não alcança a Main. Como o classes desliga essa
// busca, @TestConfiguration aninhada no teste também deixa de ser vista: precisa de @Import.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("integrationTest")
@SpringBootTest(classes = Main.class)
@Import({MySQLContainerConfiguration.class, KeycloakContainerConfiguration.class})
@ExtendWith(MySQLCleanUpExtension.class)
public @interface IntegrationTest {}
