package com.tarcisiolzbraga.codeflix.videos.infrastructure.tracing;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributesThreadLocalAccessor;

// Liga o repasse do rastro nas **duas** saídas HTTP deste serviço.
//
// Duas, e não uma: é o detalhe que decide se isto funciona. O salto medido como quebrado era
// `videos-api -> catalog-elasticsearch`, que não passa pelo `RestClient` do admin — ele sai pelo
// cliente do Elasticsearch, que o Boot autoconfigura. Consertar só o `RestClient` deixaria
// justamente o salto da tabela do README fragmentado.
@Configuration
public class TracePropagationConfig {

    // **Sem isto, o caminho do GraphQL não propaga — e ele é o caminho principal deste serviço.**
    //
    // Medido no cluster, com o repasse já ligado: a rota de mídia juntava o rastro (gateway ->
    // catálogo -> admin, num trace de 5 spans), e a consulta GraphQL continuava partida. A diferença
    // é a thread: o controller chama o admin na thread da requisição, e o graphql-java executa os
    // data fetchers em outra, onde o `RequestContextHolder` — um ThreadLocal — está vazio.
    //
    // O Spring GraphQL sabe carregar ThreadLocal para a thread de execução, mas só os que estiverem
    // registrados no `ContextRegistry`. O acessador já vem no spring-web; o que não vem é o registro,
    // porque ele não tem arquivo de serviço. Então é aqui.
    //
    // `ContextRegistry` é estático e global de propósito: é assim que a biblioteca é feita, e
    // registrar o mesmo acessador duas vezes substitui em vez de duplicar.
    @PostConstruct
    void carryTheRequestIntoTheGraphQlThreads() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new RequestAttributesThreadLocalAccessor());
    }

    // Alcança **todos** os `RestClient` construídos a partir do builder autoconfigurado, que aqui são
    // os quatro clientes da API do admin — eles compartilham um bean só.
    @Bean
    RestClientCustomizer traceOnTheAdminCalls() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            TraceHeaders.forEachInbound((nome, valor) -> request.getHeaders().set(nome, valor));
            return execution.execute(request, body);
        });
    }

    // O cliente do Elasticsearch é assíncrono e vive sobre o Apache HttpClient 5. O gancho por
    // **requisição** é um interceptor dele; `setDefaultHeaders` do `Rest5ClientBuilder` não serve,
    // porque é estático e o rastro muda a cada chamada.
    @Bean
    Rest5ClientBuilderCustomizer traceOnTheIndexCalls() {
        return new Rest5ClientBuilderCustomizer() {
            @Override
            public void customize(final co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder builder) {
                // Nada a fazer no builder de alto nível: o que interessa é o cliente HTTP abaixo.
            }

            // **Interceptor de `exec`, e não de `request`, e a diferença é a thread.** Medido:
            //
            //     exec-interceptor    -> thread "Test worker"         contexto: visível
            //     request-interceptor -> thread "httpclient-dispatch-1"  contexto: null
            //
            // O cliente é assíncrono, e o interceptor de requisição roda na thread de I/O do
            // reator — onde o `RequestContextHolder`, que é um ThreadLocal, está vazio. O de
            // `exec` roda na thread que chamou, antes de a requisição ir para o reator, e é lá
            // que a requisição de entrada ainda está visível.
            //
            // Escrevi a primeira versão com `addRequestInterceptorFirst`, e ela não propagava
            // nada. Nada quebrava: a busca funcionava, o rastro só continuava partido. O teste
            // pegou antes do deploy.
            @Override
            public void customize(final HttpAsyncClientBuilder client) {
                client.addExecInterceptorFirst(
                        "trace-propagation", (request, entityProducer, scope, chain, callback) -> {
                            TraceHeaders.forEachInbound(request::setHeader);
                            chain.proceed(request, entityProducer, scope, callback);
                        });
            }
        };
    }
}
