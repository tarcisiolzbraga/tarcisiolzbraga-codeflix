package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Dois prazos do cadastro.
//
// `validationTtl` é por quanto tempo o link de validação vale. Um dia é curto o bastante para um
// link vazado perder a validade logo, e longo o bastante para quem só abre o e-mail à noite.
//
// `retention` é por quanto tempo a linha de `pending_sign_up` **sobrevive depois de deixar de
// servir** — e isso é retenção de dado pessoal, não arrumação: a tabela guarda e-mail, nome completo
// e documento de quem pediu cadastro, inclusive de quem nunca confirmou e portanto nunca virou
// cliente.
//
// Sete dias, e o número tem razão nas duas pontas: preserva o **422** de "o link venceu" por uma
// semana depois do vencimento (apagar na hora faria essa resposta virar 404, e qual delas o cliente
// recebe passaria a depender de o limpador ter rodado), e dá a mesma semana para o clique repetido
// num link já confirmado devolver a conta em vez de erro.
@ConfigurationProperties("signup")
public record SignUpProperties(Duration validationTtl, Duration retention) {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_RETENTION = Duration.ofDays(7);

    public SignUpProperties {
        validationTtl = validationTtl == null ? DEFAULT_TTL : validationTtl;
        retention = retention == null ? DEFAULT_RETENTION : retention;
        requirePositive("signup.validation-ttl", validationTtl);
        requirePositive("signup.retention", retention);
    }

    private static void requirePositive(final String name, final Duration value) {
        if (value.isNegative() || value.isZero()) {
            throw new IllegalStateException("'%s' should be positive".formatted(name));
        }
    }
}
