package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import java.util.Arrays;
import java.util.Optional;

public enum CastMemberType {
    ACTOR,
    DIRECTOR;

    // Converte sem lançar: texto desconhecido numa mensagem replicada é assunto da validação, não
    // uma exceção que derrubaria o consumo.
    public static Optional<CastMemberType> of(final String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
