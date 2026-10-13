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

// Jornadas sobre HTTP de verdade: Tomcat em porta aleatória e MySQL em container. Diferente da fatia
// web, aqui nada é simulado — e é só aqui que o container de servlet existe, o que importa para tudo
// o que depende de despacho de erro, filtro e codificação de resposta.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("e2eTest")
@SpringBootTest(classes = Main.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(MySQLContainerConfiguration.class)
public @interface E2ETest {}
