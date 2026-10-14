package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;

// A conta dos dígitos verificadores de CPF e CNPJ. É a mesma nos dois: soma ponderada dos dígitos
// anteriores, resto por 11, e o dígito é 0 quando o resto é menor que 2. O que muda são os pesos.
//
// Fica fora do Document porque construtor compacto de record não pode chamar método de instância
// antes de os campos existirem, e porque a conta é a mesma para os dois tipos.
final class DocumentDigits {

    private DocumentDigits() {}

    static void assertDigits(final String value, final String type, final int length) {
        if (value == null || value.isBlank()) {
            throw DomainException.with("'%s' should not be empty".formatted(type));
        }
        if (value.length() != length || !value.chars().allMatch(Character::isDigit)) {
            throw DomainException.with("'%s' should have %d digits".formatted(type, length));
        }
    }

    static void assertCheckDigits(final String value, final String type, final int length) {
        if (allDigitsEqual(value) || !checkDigitsClose(value, length)) {
            throw DomainException.with("'%s' is invalid".formatted(type));
        }
    }

    // Todos os dígitos iguais fecham a conta por acidente e não são documento de ninguém.
    private static boolean allDigitsEqual(final String value) {
        return value.chars().distinct().count() == 1;
    }

    private static boolean checkDigitsClose(final String value, final int length) {
        final var base = length - 2;
        final var first = checkDigitOf(value, base);
        final var second = checkDigitOf(value, base + 1);
        return first == digitAt(value, base) && second == digitAt(value, base + 1);
    }

    // O CPF pondera de 10 e 11 para baixo; o CNPJ cicla de 2 a 9 da direita para a esquerda.
    private static int checkDigitOf(final String value, final int upTo) {
        var sum = 0;
        if (upTo <= 10) {
            for (var i = 0; i < upTo; i++) {
                sum += digitAt(value, i) * (upTo + 1 - i);
            }
        } else {
            for (var i = 0; i < upTo; i++) {
                sum += digitAt(value, i) * (2 + (upTo - 1 - i) % 8);
            }
        }
        final var remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(final String value, final int index) {
        return Character.digit(value.charAt(index), 10);
    }
}
