package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import java.net.URI;

// Confere valor de configuração que não tem padrão e precisa existir.
//
// Existe porque a ligação de @ConfigurationProperties ignora placeholder que não resolve e grava o
// próprio texto dele: diferente do @Value, variável de ambiente faltando não derruba a subida, ela
// vira o literal "${MINHA_VARIAVEL}". Em propriedade de tipo numérico isso estoura na conversão,
// mas em String passa — e a aplicação sobe apontando para lugar nenhum, falhando só no primeiro uso.
//
// A regra do projeto é derrubar a subida, como acontece no banco: é o que estes métodos fazem.
public final class ConfiguredValue {

    private static final String UNRESOLVED_MARKER = "${";
    private static final String MISSING_MESSAGE = "'%s' is required and was not set";
    private static final String UNRESOLVED_MESSAGE = "'%s' was not resolved; got '%s'. Set the environment variable.";
    private static final String NOT_A_URL_MESSAGE = "'%s' must be an absolute URL; got '%s'";

    private ConfiguredValue() {
    }

    public static String text(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(MISSING_MESSAGE.formatted(name));
        }
        if (value.contains(UNRESOLVED_MARKER)) {
            throw new IllegalStateException(UNRESOLVED_MESSAGE.formatted(name, value));
        }
        return value;
    }

    public static String url(final String name, final String value) {
        text(name, value);
        if (!isAbsoluteUrl(value)) {
            throw new IllegalStateException(NOT_A_URL_MESSAGE.formatted(name, value));
        }
        return value;
    }

    private static boolean isAbsoluteUrl(final String value) {
        try {
            final var uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }
}
