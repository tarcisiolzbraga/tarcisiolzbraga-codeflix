package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.core.annotation.AliasFor;

// Fatia GraphQL: sobe só o controller e o schema, com o GraphQlTester, sem Elasticsearch nem Kafka.
//
// Sem @Tag de propósito, ao contrário da referência, que marca esta fatia como teste de integração:
// ela não sobe container algum, então entra sozinha na task unitTests, que é por exclusão.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@GraphQlTest
public @interface GraphQLControllerTest {

    @AliasFor(annotation = GraphQlTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}
