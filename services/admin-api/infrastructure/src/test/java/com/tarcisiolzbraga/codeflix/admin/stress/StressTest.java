package com.tarcisiolzbraga.codeflix.admin.stress;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.GarageContainerConfiguration;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakContainerConfiguration;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.Main;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.MySQLContainerConfiguration;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.RabbitMQContainerConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

// Tomcat e infraestrutura de verdade, como o @E2ETest — e tag própria, **fora do `build`**.
//
// A tag é o que separa isto de um teste comum, e a razão está no nome: quem mede uma corrida falha
// quando a corrida acontece. Pôr isso no caminho de todo commit transformaria o build num sorteio,
// que é o problema sob investigação, não a solução.
//
// Sem a extensão de limpeza do banco: cada classe daqui cria o que precisa e o volume de linhas não
// é problema — o que se mede é a conexão, não o estado.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("stressTest")
@SpringBootTest(classes = Main.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({MySQLContainerConfiguration.class, GarageContainerConfiguration.class,
        RabbitMQContainerConfiguration.class, KeycloakContainerConfiguration.class})
public @interface StressTest {}
