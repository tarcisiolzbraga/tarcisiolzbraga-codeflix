package com.tarcisiolzbraga.codeflix.videos.domain.video;

import java.util.Arrays;
import java.util.Optional;

// As mesmas classificações do admin-codeflix, com os mesmos rótulos: é o texto que vem na mensagem.
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

    // Converte sem lançar: rótulo desconhecido numa mensagem replicada é erro de validação, não algo
    // que deva derrubar o consumo.
    public static Optional<Rating> of(final String value) {
        return Arrays.stream(values())
                .filter(rating -> rating.label.equalsIgnoreCase(value))
                .findFirst();
    }
}
