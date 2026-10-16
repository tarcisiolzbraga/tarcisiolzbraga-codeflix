package com.tarcisiolzbraga.codeflix.subscription.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// Prova que as classes copiadas entre os serviços **não divergiram**.
//
// Elas são cópia de propósito: são poucas linhas, estáveis, e os três serviços são implantáveis
// independentes — um build compartilhado por 74 linhas acoplaria três implantáveis por nada. A
// decisão está registrada nos comentários de cada cópia.
//
// O risco da cópia não é existir, é **divergir em silêncio**: alguém conserta um defeito num
// serviço e os outros dois seguem com ele. Aconteceu de verdade no monorepo, do lado Go — o logger
// engolia a mensagem dos erros nos dois serviços, e precisou de duas correções. Este teste troca
// esse risco por uma falha de build.
//
// Compara o **código**, não o arquivo: o pacote difere por serviço, e cada cópia tem o seu próprio
// comentário explicando o contexto dela. É o corpo que tem de ser igual.
//
// Lê os arquivos por caminho relativo, então ele é um invariante **do monorepo**: se um serviço
// algum dia sair daqui, este teste sai com ele.
class CopiedSourcesTest {

    // Sobe até achar a raiz, em vez de contar "..": o diretório de trabalho do teste é o do módulo
    // Gradle, não o do serviço, e contar níveis quebraria se alguém rodasse de outro lugar. O
    // build-logic é o marcador porque é ele que só existe na raiz.
    private static final Path MONOREPO = monorepoRoot();

    private static Path monorepoRoot() {
        var candidato = Path.of("").toAbsolutePath().normalize();
        while (candidato != null && !Files.isDirectory(candidato.resolve("build-logic"))) {
            candidato = candidato.getParent();
        }
        if (candidato == null) {
            throw new IllegalStateException("não achei a raiz do monorepo subindo a partir de "
                    + Path.of("").toAbsolutePath());
        }
        return candidato;
    }

    private static final List<String> CONVERTER_COPIES = List.of(
            "admin-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/admin/infrastructure/security/KeycloakJwtConverter.java",
            "videos-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/videos/infrastructure/security/KeycloakJwtConverter.java",
            "subscription-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/subscription/infrastructure/security/KeycloakJwtConverter.java");

    private static final List<String> CORS_CONFIG_COPIES = List.of(
            "admin-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/admin/infrastructure/configuration/CorsConfig.java",
            "videos-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/videos/infrastructure/configuration/CorsConfig.java",
            "subscription-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/subscription/infrastructure/configuration/CorsConfig.java");

    private static final List<String> CONFIGURED_VALUE_COPIES = List.of(
            "admin-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/admin/infrastructure/configuration/ConfiguredValue.java",
            "subscription-api/infrastructure/src/main/java/com/tarcisiolzbraga/codeflix/subscription/infrastructure/configuration/ConfiguredValue.java");

    @Test
    void givenTheKeycloakJwtConverterCopies_whenComparesTheirCode_thenTheyAreTheSame() throws IOException {
        assertCopiesAgree(CONVERTER_COPIES);
    }

    @Test
    void givenTheConfiguredValueCopies_whenComparesTheirCode_thenTheyAreTheSame() throws IOException {
        assertCopiesAgree(CONFIGURED_VALUE_COPIES);
    }

    @Test
    void givenTheCorsConfigCopies_whenComparesTheirCode_thenTheyAreTheSame() throws IOException {
        assertCopiesAgree(CORS_CONFIG_COPIES);
    }

    // Se o caminho mudar, o teste tem de falhar dizendo isso — e não passar por não ter achado nada
    // para comparar, que é como um teste destes morre sem ninguém notar.
    @Test
    void givenTheExpectedPaths_whenLooksForThem_thenEveryCopyIsThere() {
        Stream.of(CONVERTER_COPIES, CONFIGURED_VALUE_COPIES, CORS_CONFIG_COPIES)
                .flatMap(List::stream)
                .forEach(copy -> assertTrue(
                Files.isRegularFile(MONOREPO.resolve(copy)), "cópia não encontrada em " + copy));
    }

    private static void assertCopiesAgree(final List<String> copies) throws IOException {
        final var reference = codeOf(copies.getFirst());
        for (final var copy : copies.subList(1, copies.size())) {
            assertEquals(
                    reference,
                    codeOf(copy),
                    "o código de %s divergiu de %s. As duas são cópia de propósito: ou a correção vale para "
                                    .formatted(copy, copies.getFirst())
                            + "as duas, ou a decisão de copiar deixou de valer e elas precisam de um módulo comum.");
        }
    }

    // O código sem pacote, sem import, sem comentário e sem linha vazia, e com o segmento do serviço
    // no nome do pacote normalizado — é só isso que tem de ser igual entre as cópias.
    private static String codeOf(final String copy) throws IOException {
        try (var lines = Files.lines(MONOREPO.resolve(copy))) {
            return lines.map(String::stripTrailing)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("package ") && !line.startsWith("import "))
                    .filter(line -> !line.stripLeading().startsWith("//"))
                    .map(line -> line.replaceAll("codeflix\\.[a-z]+\\.", "codeflix.SERVICO."))
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }
}
