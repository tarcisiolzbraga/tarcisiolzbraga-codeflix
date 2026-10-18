package com.tarcisiolzbraga.codeflix.admin.infrastructure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

// O checksum de um upload de 100 MB não pode exigir os 100 MB em memória — e ele é gravado como
// metadado do objeto no armazenamento, então tem de ser conhecido **antes** do PUT.
class HashingTest {

    private static final String CONTENT = "conteudo do arquivo";

    // **A âncora:** as duas formas têm de dar o mesmo resultado. Se divergirem, um arquivo enviado
    // pela nova passa a ter checksum diferente do que a antiga gravou, e nada acusa.
    @Test
    void givenTheSameContent_whenHashedFromAnArrayAndFromAStream_thenBothGiveTheSameChecksum()
            throws IOException {
        final var fromArray = Hashing.checksumOf(CONTENT.getBytes(StandardCharsets.UTF_8));

        final var fromStream =
                Hashing.checksumOf(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)));

        assertEquals(fromArray, fromStream);
    }

    // SHA-256 tem 32 bytes, e em hexadecimal 64 caracteres. Fixado para a troca do algoritmo não
    // passar sem ninguém notar.
    @Test
    void givenAnyContent_whenHashed_thenItIsASha256InHex() throws IOException {
        final var actual = Hashing.checksumOf(new ByteArrayInputStream(CONTENT.getBytes()));

        assertEquals(64, actual.length());
        assertTrue(actual.matches("[0-9a-f]{64}"), actual);
    }

    // **A prova de que lê em blocos:** um conteúdo de 1 MB é consumido em mais de uma leitura. Com
    // `readAllBytes()` antes do digest, chegaria numa só — e o arquivo inteiro estaria em memória.
    @Test
    void givenALargeContent_whenHashed_thenItIsReadInChunksAndNotAllAtOnce() throws IOException {
        final var content = new byte[1024 * 1024];
        final var counting = new CountingInputStream(new ByteArrayInputStream(content));

        Hashing.checksumOf(counting);

        assertTrue(
                counting.reads > 1,
                "esperava leitura em blocos; o fluxo foi lido em %d chamada(s)".formatted(counting.reads));
    }

    @Test
    void givenEmptyContent_whenHashed_thenItStillGivesAChecksum() throws IOException {
        assertEquals(
                Hashing.checksumOf(new byte[0]), Hashing.checksumOf(new ByteArrayInputStream(new byte[0])));
    }

    private static final class CountingInputStream extends FilterInputStream {

        private int reads;

        private CountingInputStream(final ByteArrayInputStream source) {
            super(source);
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            this.reads++;
            return super.read(bytes, offset, length);
        }
    }
}
