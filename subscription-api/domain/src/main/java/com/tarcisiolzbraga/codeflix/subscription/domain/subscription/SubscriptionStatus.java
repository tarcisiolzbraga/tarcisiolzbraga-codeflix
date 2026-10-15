package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.Locale;

// O estado da assinatura, e para onde cada um pode ir. Continua sendo state pattern — cada estado
// declara as próprias transições —, com as constantes do enum no papel dos estados.
//
// Diverge do curso em três pontos, e os três são correção:
//
//  1. **Sem referência de volta à Subscription.** Lá o estado guarda a assinatura e chama
//     `subscription.execute(...)`, que reconstrói o estado: os dois apontam um para o outro, e
//     testar um exige montar o outro. Aqui a transição devolve o próximo estado e o agregado faz
//     `this.status = this.status.activate()`.
//  2. **Transição não declarada é recusa, não silêncio.** A classe base do curso implementa as
//     quatro transições como no-op vazio, então esquecer de sobrescrever uma passa batido. Aqui o
//     padrão lança, e cada constante declara o que permite — inclusive o próprio caso idempotente.
//  3. **Não existe transição para trailing.** Nada volta para ele: é só o estado inicial. No curso
//     há um método `trailing()` que em todo estado ou não faz nada ou lança — código morto com cara
//     de regra.
//
// O texto guardado é o mesmo do curso, em minúsculas, então um banco criado seguindo a referência
// continua legível por aqui.
public enum SubscriptionStatus {
    TRAILING {
        @Override
        public SubscriptionStatus activate() {
            return ACTIVE;
        }

        @Override
        public SubscriptionStatus markIncomplete() {
            return INCOMPLETE;
        }
    },
    ACTIVE {
        // Idempotente: a cobrança pode ser tentada de novo, e o mesmo resultado não derruba nada.
        @Override
        public SubscriptionStatus activate() {
            return ACTIVE;
        }

        @Override
        public SubscriptionStatus markIncomplete() {
            return INCOMPLETE;
        }
    },
    INCOMPLETE {
        @Override
        public SubscriptionStatus activate() {
            return ACTIVE;
        }

        @Override
        public SubscriptionStatus markIncomplete() {
            return INCOMPLETE;
        }
    },
    // Terminal, e é a regra que protege o assinante de voltar a ser cobrado por algo que encerrou.
    // Só o cancelar, que o padrão abaixo já resolve, é aceito.
    CANCELED;

    public SubscriptionStatus activate() {
        throw refuses("active");
    }

    public SubscriptionStatus markIncomplete() {
        throw refuses("incomplete");
    }

    // Cancelar vale de qualquer estado: é o que o assinante pede, e o que a cobrança faz quando o
    // prazo de tolerância acaba. Cancelar o que já está cancelado não é erro.
    public SubscriptionStatus cancel() {
        return CANCELED;
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SubscriptionStatus of(final String value) {
        if (value == null || value.isBlank()) {
            throw DomainException.with("'status' should not be empty");
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw DomainException.with("'%s' is not a valid subscription status".formatted(value));
        }
    }

    private DomainException refuses(final String target) {
        return DomainException.with("a %s subscription cannot become %s".formatted(value(), target));
    }
}
