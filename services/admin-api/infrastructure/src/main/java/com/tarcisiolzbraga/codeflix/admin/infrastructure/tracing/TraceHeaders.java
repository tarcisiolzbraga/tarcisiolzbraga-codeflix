package com.tarcisiolzbraga.codeflix.admin.infrastructure.tracing;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Os cabeçalhos que carregam o rastro, e de onde eles são lidos.
//
// **Por que isto existe.** O Istio gera os spans sozinho, mas ele não consegue ligar a chamada de
// saída à requisição que a originou: o sidecar de saída vê um pedido **sem** cabeçalho de rastro e
// começa um rastro novo. O `deploy/README.md` tem a medição que mostrou isso, feita no catálogo.
//
// Daqui a saída é o armazenamento: todo upload e todo download de mídia passa pelo Garage. Sem o
// repasse, "o download de 400 MB levou 30 s" não dizia se o tempo era desta API ou do
// armazenamento — e essa foi a pergunta de cada investigação de mídia deste repositório.
//
// **Repetido, e não compartilhado.** Os serviços têm builds independentes, e um módulo comum para
// uma lista de cabeçalhos e dez linhas de leitura custaria mais que a repetição — a mesma razão que
// o `MediaContent` do catálogo registra.
//
// O trabalho da aplicação é só não quebrar a corrente: repassar na saída o que recebeu na entrada.
// Os spans continuam sendo do Istio, e nada aqui os produz.
//
// **A lista é um superconjunto de propósito.** O tracer do Envoy nesta malha é o
// `envoy.tracers.opentelemetry` (conferido no `config_dump` do sidecar), que propaga W3C
// `traceparent`. O B3 e o `x-ot-span-context` entram porque o Istio os gera em outras configurações
// de tracer, e trocar o provedor não deveria quebrar a propagação de novo, calado. Cabeçalho que não
// veio não é repassado, então o custo de listar demais é zero.
public final class TraceHeaders {

    private static final List<String> NOMES = List.of(
            // W3C: o que esta malha usa hoje
            "traceparent",
            "tracestate",
            // o identificador que o Istio põe em toda requisição, e que o Kiali e os logs usam
            "x-request-id",
            // B3, nas duas formas — o Istio gera a multi-cabeçalho e aceita a única
            "x-b3-traceid",
            "x-b3-spanid",
            "x-b3-parentspanid",
            "x-b3-sampled",
            "x-b3-flags",
            "b3",
            // legado do Lightstep, que o Istio ainda repassa
            "x-ot-span-context");

    private TraceHeaders() {}

    // Entrega cada cabeçalho de rastro da requisição em curso.
    //
    // **Fora de uma requisição HTTP não faz nada, e isso é o caso comum e não exceção:** o relay da
    // outbox e os listeners de evento deste serviço tocam o armazenamento sem requisição nenhuma por
    // trás. Lá não há rastro para continuar, então não há o que propagar.
    public static void forEachInbound(final BiConsumer<String, String> destination) {
        inboundRequest().ifPresent(request -> {
            for (final var nome : NOMES) {
                final var valor = request.getHeader(nome);
                if (valor != null) {
                    destination.accept(nome, valor);
                }
            }
        });
    }

    private static Optional<jakarta.servlet.http.HttpServletRequest> inboundRequest() {
        // `getRequestAttributes` devolve nulo fora de requisição, e é assim que se pergunta: não há
        // `isRequestActive` no Spring, e segurar a requisição num campo quebraria com o pool de
        // threads do Tomcat.
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos
                ? Optional.of(atributos.getRequest())
                : Optional.empty();
    }
}
