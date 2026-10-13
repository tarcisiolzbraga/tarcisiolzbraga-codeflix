package com.tarcisiolzbraga.codeflix.subscription.domain;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;

// Os ids guardam UUID, e não texto, como nos outros dois serviços Java do monorepo. Diverge do
// curso, que usa CHAR(32) sem hífen em Account e Subscription e auto-increment em Plan: três
// formas de id no mesmo repositório custariam mais do que o contrato do `planId` sair de número
// para texto no JSON.
public interface Identifier extends ValueObject {

    // O valor tipado. Os ids são records de um UUID, então o acessor do record já implementa isto
    // sem nenhum código.
    UUID value();

    // A forma canônica em texto, para quem precisa dela: coluna, JSON, rota, mensagem de erro. É
    // sempre minúscula, porque é o toString() do UUID.
    default String getValue() {
        return value().toString();
    }

    // Converte no limite do domínio: id que vem de fora — rota, payload, evento de CDC — ou é UUID
    // aqui, ou não entra. Fica nesta interface porque todos os ids fazem exatamente isto, e um
    // formato recusado tem de ler igual em todos.
    //
    // Erro de domínio, nunca exceção de framework: o IllegalArgumentException do UUID chegaria ao
    // Spring como 500, e isto é entrada inválida. Virando DomainException, o handler da API já
    // responde 422, como qualquer outra recusa daqui.
    static UUID uuidOf(final String value, final Class<? extends Identifier> type) {
        if (value == null) {
            throw invalid(value, type);
        }
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException exception) {
            throw invalid(value, type);
        }
    }

    private static DomainException invalid(final String value, final Class<? extends Identifier> type) {
        return DomainException.with("'%s' is not a valid %s".formatted(value, type.getSimpleName()));
    }
}
