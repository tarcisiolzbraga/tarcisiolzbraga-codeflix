package com.tarcisiolzbraga.codeflix.admin.infrastructure.tracing;

import java.util.Objects;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

// Põe o rastro na chamada ao armazenamento, **depois** de o SDK a ter assinado.
//
// **A ordem aqui não é preferência, é a única que funciona — e eu descobri do jeito difícil.** A
// primeira versão usava um `ExecutionInterceptor` com `modifyHttpRequest`, que roda antes da
// assinatura. O cabeçalho entrava no SigV4:
//
//     SignedHeaders=amz-sdk-invocation-id;amz-sdk-request;host;traceparent;x-amz-content-sha256;...
//
// E aí o Garage recusava com `403 Invalid signature`, em **todo** pedido de mídia. A razão é que o
// sidecar do Istio **reescreve** `traceparent` e `x-request-id` em trânsito: ele cria um span novo
// para o salto e troca o valor. Assinatura sobre um cabeçalho que a malha reescreve nunca
// corresponde do outro lado.
//
// Então o rastro entra na camada do cliente HTTP, que o SDK chama depois de assinar: ele vai na
// requisição, não vai na assinatura, e a malha pode reescrevê-lo à vontade.
//
// **O teste anterior não pegou isso, e vale saber por quê:** ele afirmava que a requisição continua
// assinada, e continuava — o dublê não confere assinatura. A afirmação era verdadeira e inútil. O
// teste de agora fixa que `traceparent` **não** está em `SignedHeaders`, que é o que importa.
public final class TracePropagatingHttpClient implements SdkHttpClient {

    private final SdkHttpClient delegate;

    public TracePropagatingHttpClient(final SdkHttpClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be null");
    }

    // Um `SdkHttpClient.Builder` que embrulha outro, para o SDK continuar aplicando os padrões do
    // serviço — passar um cliente já construído por `httpClient(...)` os descartaria.
    public static Builder wrapping(final SdkHttpClient.Builder<?> delegate) {
        return new Builder(delegate);
    }

    // Classe nomeada, e não lambda: `SdkHttpClient.Builder<T>` tem limite recursivo
    // (`T extends Builder<T>`), e um lambda não consegue satisfazê-lo.
    public static final class Builder implements SdkHttpClient.Builder<Builder> {

        private final SdkHttpClient.Builder<?> delegate;

        private Builder(final SdkHttpClient.Builder<?> delegate) {
            this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be null");
        }

        @Override
        public SdkHttpClient buildWithDefaults(final AttributeMap serviceDefaults) {
            return new TracePropagatingHttpClient(this.delegate.buildWithDefaults(serviceDefaults));
        }
    }

    @Override
    public ExecutableHttpRequest prepareRequest(final HttpExecuteRequest request) {
        final var comRastro = request.httpRequest().toBuilder();
        TraceHeaders.forEachInbound(comRastro::putHeader);
        // Reconstruída à mão porque `HttpExecuteRequest` não tem `toBuilder`: o corpo e o coletor de
        // métrica vêm junto, senão um upload sairia sem conteúdo.
        final var reconstruida = HttpExecuteRequest.builder().request(comRastro.build());
        request.contentStreamProvider().ifPresent(reconstruida::contentStreamProvider);
        request.metricCollector().ifPresent(reconstruida::metricCollector);
        return this.delegate.prepareRequest(reconstruida.build());
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    @Override
    public String clientName() {
        return this.delegate.clientName();
    }
}
