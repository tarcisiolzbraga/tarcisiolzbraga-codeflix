package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
// A tag separa quem precisa de Docker: a task unitTests exclui esta e a do e2e.
@Tag("integrationTest")
// classes explícito porque os UseCaseIT ficam no pacote da application, de onde a busca por
// @SpringBootConfiguration subindo pelos pacotes não alcança a Main. Como o classes desliga essa
// busca, @TestConfiguration aninhada no teste também deixa de ser vista: precisa de @Import.
@SpringBootTest(classes = Main.class)
@Import(MySQLContainerConfiguration.class)
@ExtendWith(MySQLCleanUpExtension.class)
public @interface IntegrationTest {

    // MOCK por padrão; RANDOM_PORT sobe o Tomcat de verdade, para testar o que passa por ele.
    @AliasFor(annotation = SpringBootTest.class, attribute = "webEnvironment")
    WebEnvironment webEnvironment() default WebEnvironment.MOCK;
}
