package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadsIT.ThreadRouteConfiguration.class)
class VirtualThreadsIT {

    private static final String THREAD_PATH = "/test/thread";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    @Test
    void givenRunningServer_whenCallAnEndpoint_thenHandleItInAVirtualThread() {
        // A rota é só deste teste e cai na regra geral da API, que exige role: daí o token.
        final var client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .build();

        final var actualIsVirtual = client.get().uri(THREAD_PATH).retrieve().body(String.class);

        assertEquals("true", actualIsVirtual);
    }

    // Rota só deste teste: responde se o Tomcat atendeu a requisição numa virtual thread.
    @TestConfiguration(proxyBeanMethods = false)
    static class ThreadRouteConfiguration {

        @Bean
        RouterFunction<ServerResponse> threadRoute() {
            return RouterFunctions.route()
                    .GET(THREAD_PATH, request -> ServerResponse.ok()
                            .body(String.valueOf(Thread.currentThread().isVirtual())))
                    .build();
        }
    }
}
