package com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Quantas requisições por origem, em dois degraus.
//
// `signUp` é apertado porque aquelas rotas são as abertas, e cada chamada custa caro ao sistema:
// uma cria usuário no Keycloak e manda e-mail, a outra é onde um token de validação seria
// adivinhado por tentativa. `general` é folgado, e existe para pôr teto em tudo o mais sem estorvar
// quem usa a API de verdade — inclusive quem está atrás de um NAT compartilhado, onde muita gente
// divide uma origem.
@ConfigurationProperties("rate-limit")
// `Boolean`, e não `boolean`: o primitivo cairia para `false` quando a propriedade faltasse, e o
// limitador nasceria **desligado em silêncio** — o oposto do que este projeto faz com configuração
// ausente. Com o objeto dá para distinguir "não definido" de "definido como falso", e o não
// definido liga.
public record RateLimitProperties(Boolean enabled, Tier signUp, Tier confirm, Tier general) {

    private static final Tier DEFAULT_SIGN_UP = new Tier(10, Duration.ofMinutes(10));
    private static final Tier DEFAULT_CONFIRM = new Tier(20, Duration.ofMinutes(10));
    private static final Tier DEFAULT_GENERAL = new Tier(120, Duration.ofMinutes(1));

    public RateLimitProperties {
        enabled = enabled == null || enabled;
        signUp = signUp == null ? DEFAULT_SIGN_UP : signUp;
        confirm = confirm == null ? DEFAULT_CONFIRM : confirm;
        general = general == null ? DEFAULT_GENERAL : general;
    }

    // O maior período em uso, que é o que o registro de baldes precisa saber para expirar sem
    // devolver fichas antes da hora.
    public Duration longestPeriod() {
        var longest = this.signUp.period();
        if (this.confirm.period().compareTo(longest) > 0) {
            longest = this.confirm.period();
        }
        return this.general.period().compareTo(longest) > 0 ? this.general.period() : longest;
    }

    // Um degrau: quantas fichas o balde guarda e em quanto tempo ele se enche de novo.
    public record Tier(int capacity, Duration period) {

        public Tier {
            if (capacity <= 0) {
                throw new IllegalStateException("'rate-limit.*.capacity' should be positive");
            }
            if (period == null || period.isNegative() || period.isZero()) {
                throw new IllegalStateException("'rate-limit.*.period' should be positive");
            }
        }
    }
}
