package com.tarcisiolzbraga.codeflix.subscription.infrastructure.tracing;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Liga o repasse do rastro na saída HTTP deste serviço.
//
// Uma só, diferente do catálogo: aqui todas as chamadas de saída vão ao Keycloak, e todas passam
// pelo `RestClient` construído a partir do builder autoconfigurado. O armazenamento e o banco deste
// serviço não falam HTTP.
@Configuration
public class TracePropagationConfig {

    @Bean
    RestClientCustomizer traceOnTheKeycloakCalls() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            TraceHeaders.forEachInbound((nome, valor) -> request.getHeaders().set(nome, valor));
            return execution.execute(request, body);
        });
    }
}
