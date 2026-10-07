package com.tarcisiolzbraga.codeflix.admin.domain;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import java.util.Objects;
import java.util.UUID;

public interface Identifier {

    // O valor tipado. Os quatro ids são records de um UUID, então o acessor do record já implementa
    // isto sem nenhum código.
    UUID value();

    // A forma canônica em texto, para quem precisa dela: coluna, JSON, rota, mensagem de erro. É
    // sempre minúscula, porque é o toString() do UUID.
    default String getValue() {
        return value().toString();
    }

    // Converte no limite do domínio: id que vem de fora — rota, payload, evento de CDC — ou é UUID
    // aqui, ou não entra. Fica nesta interface porque os quatro ids fazem exatamente isto, e um
    // formato recusado tem de ler igual nos quatro.
    //
    // Erro de domínio, nunca exceção de framework: o IllegalArgumentException do UUID chegaria ao
    // Spring como 500, e isto é entrada inválida. Virando DomainException, o handler da API já
    // responde 422, como qualquer outra validação daqui.
    static UUID uuidOf(final String value, final Class<? extends Identifier> type) {
        Objects.requireNonNull(value, "'value' should not be null");
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException exception) {
            throw DomainException.with(
                    new ValidationError("'%s' is not a valid %s".formatted(value, type.getSimpleName())));
        }
    }
}
