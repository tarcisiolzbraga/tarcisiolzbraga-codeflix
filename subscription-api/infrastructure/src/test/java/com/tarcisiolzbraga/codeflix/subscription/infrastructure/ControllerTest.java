package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.annotation.AliasFor;

// Fatia web: sobe só o controller, o advice e o MockMvc, sem banco nem Testcontainers. Aqui se
// verifica o contrato — status, corpo e o que chega ao caso de uso. Quem alcança cada rota é assunto
// do SecurityConfigTest, e o que depende de despacho de erro é do E2E.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WebMvcTest
public @interface ControllerTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}
