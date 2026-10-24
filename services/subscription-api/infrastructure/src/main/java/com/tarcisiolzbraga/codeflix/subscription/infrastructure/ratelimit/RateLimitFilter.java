package com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.SecurityConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

// O teto de requisição por origem, antes de a requisição chegar a controller algum. Três degraus,
// um por preço: pedir cadastro, confirmar cadastro, e todo o resto.
//
// Filtro, e não interceptor nem anotação em cada rota: o que se quer barrar é a chamada, não a
// execução dela — recusar depois de o Spring montar o corpo e resolver o caso de uso já teria
// gastado o que o limite existe para poupar.
//
// **O contador é por instância, e isso está declarado em vez de escondido.** Com duas réplicas, o
// teto efetivo dobra, porque cada uma tem os seus baldes. Contador compartilhado pediria Redis, que
// este projeto não tem; o bucket4j suporta e a troca é de implementação do registro, não do filtro.
// Para o que este limite protege — cadastro em massa e adivinhação de token —, dobrar um teto de 5
// em 10 minutos ainda é um teto.
//
// A origem vem do `getRemoteAddr()`, que atrás do gateway só é a de verdade com o
// `forward-headers-strategy` ligado (ver os perfis homolog e production). E aí vale dizer o limite:
// quem controla o `X-Forwarded-For` escolhe a própria chave e escapa do balde. Quem sanitiza esse
// cabeçalho é o gateway, e é nele que a defesa de borda mora — este filtro é a de base.
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String SIGN_UP_TIER = "sign-up";
    private static final String CONFIRM_TIER = "confirm";
    private static final String GENERAL_TIER = "general";

    private final RateLimitProperties properties;
    private final BucketRegistry registry;
    private final ObjectMapper mapper;

    public RateLimitFilter(
            final RateLimitProperties properties,
            final BucketRegistry registry,
            final ObjectMapper mapper) {
        this.properties = Objects.requireNonNull(properties, "'properties' should not be null");
        this.registry = Objects.requireNonNull(registry, "'registry' should not be null");
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws ServletException, IOException {
        final var probe = bucketFor(request).tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        refuse(response, Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    // A porta de gerenciamento fica fora: as sondas batem de dez em dez segundos, e limitá-las
    // faria o orquestrador declarar o pod morto por causa do limitador.
    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !this.properties.enabled() || isManagementPort(request);
    }

    // As duas rotas do cadastro vêm nomeadas do SecurityConfig, e não de uma cópia da lista: é o
    // "duas listas, não uma" do README. Rota aberta nova lá sem degrau aqui cai no `general`, que é
    // teto de verdade — não passa sem limite nenhum.
    private Bucket bucketFor(final HttpServletRequest request) {
        final var client = request.getRemoteAddr();
        final var uri = request.getRequestURI();
        if (SecurityConfig.SIGN_UP_PATH.equals(uri)) {
            return this.registry.bucketOf(SIGN_UP_TIER, client, this.properties.signUp());
        }
        if (SecurityConfig.SIGN_UP_CONFIRM_PATH.equals(uri)) {
            return this.registry.bucketOf(CONFIRM_TIER, client, this.properties.confirm());
        }
        return this.registry.bucketOf(GENERAL_TIER, client, this.properties.general());
    }

    private boolean isManagementPort(final HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    // Retry-After em segundos, que é o que o cabeçalho aceita, arredondado para cima: dizer "0"
    // convidaria a tentar de novo na hora.
    private void refuse(final HttpServletResponse response, final Duration wait) throws IOException {
        final var seconds = Math.max(1, wait.plusSeconds(1).minusNanos(1).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        final var body = new ApiError("Too many requests. Retry in %d second(s).".formatted(seconds));
        response.getWriter().write(this.mapper.writeValueAsString(body));
    }
}
