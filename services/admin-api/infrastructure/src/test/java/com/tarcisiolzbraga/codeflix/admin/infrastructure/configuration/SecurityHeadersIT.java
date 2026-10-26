package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakTestToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

// Os cabeçalhos de segurança continuam saindo, e este teste existe por causa de uma troca.
//
// O `HeaderWriterFilter` do Spring Security passou a escrever **antes** da cadeia, e não depois,
// porque escrever depois estourava na rota de mídia: com despacho assíncrono, "depois da cadeia" não
// é "depois da resposta", e o filtro ia ler cabeçalho de um `Response` já reciclado pelo Tomcat.
//
// O preço de escrever antes é que um handler que ponha o mesmo cabeçalho depois o sobrescreve, em vez
// de o `hasHeader` do writer o respeitar. Nenhuma rota daqui define esses cabeçalhos, então a troca é
// de ordem e não de conteúdo — e é isto que fixa isso, para a mudança não virar "consertei
// desligando a proteção".
@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityHeadersIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    @Test
    void givenAnyAuthorizedRequest_whenItAnswers_thenTheSecurityHeadersAreThere() {
        final var resposta = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .build()
                .get()
                .uri("/categories")
                .retrieve()
                .toEntity(byte[].class);

        final var cabecalhos = resposta.getHeaders();
        assertEquals("nosniff", cabecalhos.getFirst("X-Content-Type-Options"));
        assertEquals("DENY", cabecalhos.getFirst("X-Frame-Options"));
        assertEquals("0", cabecalhos.getFirst("X-XSS-Protection"));
        assertNotNull(cabecalhos.getCacheControl(), "faltou o Cache-Control");
        assertEquals("no-cache", cabecalhos.getFirst("Pragma"));
        assertEquals("0", cabecalhos.getFirst("Expires"));
    }
}
