package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit.BucketRegistry;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit.RateLimitFilter;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

// O limitador entra na cadeia de filtros **antes** do Spring Security, e isso é a escolha que
// importa aqui: recusar depois da autenticação já teria gasto a validação do token e a ida ao JWKS,
// que é parte do que o limite existe para poupar. As rotas que ele mais protege são justamente as
// que não têm token nenhum.
//
// O ForwardedHeaderFilter do Boot é registrado em HIGHEST_PRECEDENCE, então quando o
// forward-headers-strategy está ligado ele roda antes deste e o getRemoteAddr() já devolve a origem
// de verdade. A ordem entre os dois não é coincidência: invertida, o limitador veria sempre o IP do
// gateway e viraria um teto global.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    BucketRegistry bucketRegistry(final RateLimitProperties properties) {
        return new BucketRegistry(properties.longestPeriod());
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            final RateLimitProperties properties, final BucketRegistry registry, final ObjectMapper mapper) {
        final var registration = new FilterRegistrationBean<>(new RateLimitFilter(properties, registry, mapper));
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 10);
        return registration;
    }
}
