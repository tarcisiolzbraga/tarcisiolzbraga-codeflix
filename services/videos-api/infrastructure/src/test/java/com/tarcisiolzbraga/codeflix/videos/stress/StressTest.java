package com.tarcisiolzbraga.codeflix.videos.stress;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.ElasticsearchContainerConfiguration;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KafkaContainerConfiguration;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakContainerConfiguration;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.Main;
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
// A tag é a mesma do irmão na admin-api, e pela mesma razão: quem mede uma corrida falha quando a
// corrida acontece, e pôr isso no caminho de todo commit transformaria o build num sorteio.
//
// Sem a extensão de limpeza do índice: o que se mede aqui é a conexão com o admin e o despacho
// assíncrono do Tomcat, e o caminho medido nem consulta o Elasticsearch.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("stressTest")
@SpringBootTest(classes = Main.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({ElasticsearchContainerConfiguration.class, KafkaContainerConfiguration.class,
        KeycloakContainerConfiguration.class})
public @interface StressTest {}
