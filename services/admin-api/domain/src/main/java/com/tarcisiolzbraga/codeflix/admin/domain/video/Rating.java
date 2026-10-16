package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Arrays;
import java.util.Optional;

// A classificação indicativa: o nome da constante é o do Java, e o rótulo é o que a API expõe,
// porque "10" e "12" não podem ser nome de constante.
public enum Rating {
    ER("ER"),
    L("L"),
    AGE_10("10"),
    AGE_12("12"),
    AGE_14("14"),
    AGE_16("16"),
    AGE_18("18");

    private final String label;

    Rating(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    // Converte sem lançar: rótulo desconhecido é assunto da validação, não uma exceção.
    public static Optional<Rating> of(final String value) {
        return Arrays.stream(values())
                .filter(rating -> rating.label.equalsIgnoreCase(value))
                .findFirst();
    }
}
