package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;

// CPF e CNPJ conferidos pelos dígitos verificadores, não só pelo comprimento como na referência:
// por lá "11111111111" e "00000000000" passavam, e documento plainly inválido entrava no banco.
//
// Guarda só dígitos. Pontuação é recusada em vez de normalizada: quem chama a API manda o número, e
// uma mensagem dizendo quantos dígitos são esperados resolve mais rápido que um valor transformado
// em silêncio.
public sealed interface Document extends ValueObject {

    String value();

    String type();

    static Document create(final String documentNumber, final String documentType) {
        if (documentType == null || documentType.isBlank()) {
            throw DomainException.with("'documentType' should not be empty");
        }
        return switch (documentType) {
            case Cpf.TYPE -> new Cpf(documentNumber);
            case Cnpj.TYPE -> new Cnpj(documentNumber);
            default -> throw DomainException.with("'%s' is not a valid document type".formatted(documentType));
        };
    }

    record Cpf(String value) implements Document {

        public static final String TYPE = "cpf";
        private static final int LENGTH = 11;

        public Cpf {
            DocumentDigits.assertDigits(value, TYPE, LENGTH);
            DocumentDigits.assertCheckDigits(value, TYPE, LENGTH);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    record Cnpj(String value) implements Document {

        public static final String TYPE = "cnpj";
        private static final int LENGTH = 14;

        public Cnpj {
            DocumentDigits.assertDigits(value, TYPE, LENGTH);
            DocumentDigits.assertCheckDigits(value, TYPE, LENGTH);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }
}
