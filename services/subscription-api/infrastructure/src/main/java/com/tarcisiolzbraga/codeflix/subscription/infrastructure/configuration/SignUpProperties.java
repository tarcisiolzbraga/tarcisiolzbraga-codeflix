package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Por quanto tempo o link de validação vale. Um dia é curto o bastante para um link vazado perder
// a validade logo, e longo o bastante para quem só abre o e-mail à noite.
@ConfigurationProperties("signup")
public record SignUpProperties(Duration validationTtl) {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public SignUpProperties {
        validationTtl = validationTtl == null ? DEFAULT_TTL : validationTtl;
        if (validationTtl.isNegative() || validationTtl.isZero()) {
            throw new IllegalStateException("'signup.validation-ttl' should be positive");
        }
    }
}
