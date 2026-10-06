package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import java.util.Arrays;
import java.util.Optional;

public enum CastMemberType {
    ACTOR,
    DIRECTOR;

    // Converte sem lançar: texto desconhecido é assunto da validação, não uma exceção.
    public static Optional<CastMemberType> of(final String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
