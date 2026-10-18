package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

// O conteúdo de um `Resource` é uma **fonte**, e não um arranjo. Por isso saíram daqui os dois testes
// de cópia defensiva e o de igualdade por bytes: não há arranjo a proteger, e comparar duas fontes
// exigiria abrir e ler as duas.
//
// O que entrou em troca é o que a fonte promete: pode ser aberta **mais de uma vez**, e é isso que o
// envio exige — uma passada calcula o checksum, que vai como metadado do objeto, e outra envia.
class ResourceTest {

    private static final String CONTENT = "conteudo";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateIt() throws IOException {
        final var actual = Resource.with(CONTENT.getBytes(), "abc1", "video/mp4", "duna.mp4");

        assertEquals("abc1", actual.checksum());
        assertEquals("video/mp4", actual.contentType());
        assertEquals("duna.mp4", actual.name());
        assertEquals(CONTENT, read(actual));
    }

    // **A propriedade que o envio depende:** o checksum precisa de uma passada e o PUT de outra, e a
    // mesma fonte serve as duas. Se ela só pudesse ser aberta uma vez, o arquivo teria de ficar em
    // memória — que é exatamente o custo que esta mudança remove.
    @Test
    void givenAResource_whenTheSourceIsOpenedTwice_thenItDeliversTheSameContentBothTimes() throws IOException {
        final var actual = Resource.with(CONTENT.getBytes(), "abc1", "video/mp4", "duna.mp4");

        assertEquals(CONTENT, read(actual));
        assertEquals(CONTENT, read(actual));
    }

    // O tamanho vem junto porque o PUT do S3 o exige: sem `Content-Length` o SDK bufferiza o corpo
    // para descobri-lo, o que anularia a troca.
    @Test
    void givenAResource_whenAskedForTheLength_thenItKnowsItWithoutReading() {
        final var actual = Resource.with(CONTENT.getBytes(), "abc1", "video/mp4", "duna.mp4");

        assertEquals(CONTENT.length(), actual.content().length());
    }

    // A conveniência de arranjo copia na entrada: quem passou o arranjo não muda o que o `Resource`
    // guardou. É a última cópia defensiva que sobrou, e ela está no lugar certo — na fábrica que
    // recebe um arranjo mutável, e não em toda leitura.
    @Test
    void givenAnArraySource_whenTheCallerChangesTheArrayItPassed_thenKeepTheOriginalContent() throws IOException {
        final var content = CONTENT.getBytes();
        final var actual = Resource.with(content, "abc1", "video/mp4", "duna.mp4");

        content[0] = 'X';

        assertEquals(CONTENT, read(actual));
    }

    @Test
    void givenNullContent_whenCallWith_thenReceiveAnError() {
        final var actual = assertThrows(
                NullPointerException.class, () -> Resource.with((MediaSource) null, "abc1", "video/mp4", "duna.mp4"));

        assertEquals("'content' should not be null", actual.getMessage());
    }

    @Test
    void givenNullChecksum_whenCallWith_thenReceiveAnError() {
        final var actual = assertThrows(
                NullPointerException.class, () -> Resource.with(CONTENT.getBytes(), null, "video/mp4", "duna.mp4"));

        assertEquals("'checksum' should not be null", actual.getMessage());
    }

    @Test
    void givenNullContentType_whenCallWith_thenReceiveAnError() {
        final var actual = assertThrows(
                NullPointerException.class, () -> Resource.with(CONTENT.getBytes(), "abc1", null, "duna.mp4"));

        assertEquals("'contentType' should not be null", actual.getMessage());
    }

    @Test
    void givenNullName_whenCallWith_thenReceiveAnError() {
        final var actual = assertThrows(
                NullPointerException.class, () -> Resource.with(CONTENT.getBytes(), "abc1", "video/mp4", null));

        assertEquals("'name' should not be null", actual.getMessage());
    }

    @Test
    void givenAnArraySource_whenRead_thenTheBytesAreTheOnesGiven() throws IOException {
        final var actual = Resource.with(new byte[] {1, 2, 3}, "abc1", "video/mp4", "duna.mp4");

        try (var content = actual.content().open()) {
            assertArrayEquals(new byte[] {1, 2, 3}, content.readAllBytes());
        }
    }

    private static String read(final Resource resource) throws IOException {
        try (var content = resource.content().open()) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
