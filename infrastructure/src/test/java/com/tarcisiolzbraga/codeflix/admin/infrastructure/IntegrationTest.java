package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@Import(MySQLContainerConfiguration.class)
public @interface IntegrationTest {

    // MOCK por padrão; RANDOM_PORT sobe o Tomcat de verdade, para testar o que passa por ele.
    @AliasFor(annotation = SpringBootTest.class, attribute = "webEnvironment")
    WebEnvironment webEnvironment() default WebEnvironment.MOCK;
}
