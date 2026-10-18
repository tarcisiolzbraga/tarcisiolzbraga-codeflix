package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaContent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class S3StorageServiceIT {

    @Autowired
    private StorageService storageService;

    @Test
    void givenAResource_whenStoreAndGet_thenReceiveItBackWholeWithItsMetadata() throws IOException {
        final var name = uniqueName();
        final var expectedResource = resource();

        this.storageService.store(name, expectedResource);

        final var actual = this.storageService.get(name);
        assertTrue(actual.isPresent());
        assertSameResource(expectedResource, actual.get());
    }

    @Test
    void givenAnUnknownName_whenGet_thenReceiveEmpty() {
        final var actualResource = this.storageService.get(uniqueName());

        assertTrue(actualResource.isEmpty());
    }

    @Test
    void givenAStoredResource_whenDelete_thenItIsGone() {
        final var name = uniqueName();
        this.storageService.store(name, resource());

        this.storageService.delete(Set.of(name));

        assertTrue(this.storageService.get(name).isEmpty());
    }

    @Test
    void givenNoNames_whenDelete_thenDoNothing() {
        final var name = uniqueName();
        this.storageService.store(name, resource());

        this.storageService.delete(Set.of());

        assertTrue(this.storageService.get(name).isPresent());
    }

    // O trecho, contra o armazenamento de verdade. É aqui que se confere a afirmação em que o resto
    // do caminho se apoia: **o S3 entende o cabeçalho `bytes=` e devolve o `Content-Range` pronto**,
    // com o intervalo servido e o tamanho total. Se isso não fosse verdade, a rota teria de refazer a
    // conta do intervalo.
    @Test
    void givenAStoredResource_whenGetSlice_thenReceiveOnlyThoseBytesAndTheContentRange() {
        final var name = uniqueName();
        this.storageService.store(name, resource());

        final var actual = this.storageService.getSlice(name, "bytes=0-7");

        assertTrue(actual.isPresent());
        assertEquals("conteudo", writtenBy(actual.get().content()));
        // "conteudo do arquivo" tem 19 bytes, e o total vem do próprio armazenamento.
        assertEquals("bytes 0-7/19", actual.get().contentRange());
        assertEquals("video/mp4", actual.get().contentType());
    }

    // `bytes=10-` é o que um player manda ao buscar na linha de tempo: do ponto até o fim, sem saber
    // onde o fim está.
    @Test
    void givenAnOpenEndedRange_whenGetSlice_thenReceiveFromThereToTheEnd() {
        final var name = uniqueName();
        this.storageService.store(name, resource());

        final var actual = this.storageService.getSlice(name, "bytes=9-");

        assertTrue(actual.isPresent());
        assertEquals("do arquivo", writtenBy(actual.get().content()));
        assertEquals("bytes 9-18/19", actual.get().contentRange());
    }

    @Test
    void givenAnUnknownName_whenGetSlice_thenReceiveEmpty() {
        assertTrue(this.storageService.getSlice(uniqueName(), "bytes=0-7").isEmpty());
    }

    // Trecho que o arquivo não tem **não é** arquivo ausente: virar Optional vazio daria 404, que
    // mentiria — o arquivo está lá.
    @Test
    void givenARangeBeyondTheFile_whenGetSlice_thenFailSayingTheRangeDoesNotExist() {
        final var name = uniqueName();
        this.storageService.store(name, resource());

        assertThrows(
                RangeNotSatisfiableException.class, () -> this.storageService.getSlice(name, "bytes=5000-6000"));
    }

    private String uniqueName() {
        return UUID.randomUUID().toString();
    }

    private Resource resource() {
        return Resource.with("conteudo do arquivo".getBytes(), "abc123", "video/mp4", "duna.mp4");
    }

    // O trecho é escrito, e não lido: o conteúdo é um `MediaContent`, que existe para o arquivo não
    // passar inteiro pela memória. Escrever num destino e comparar o que saiu é afirmar a mesma
    // coisa que antes — e, de passagem, prova que o fluxo do SDK entrega o que prometeu.
    private static String writtenBy(final MediaContent content) {
        final var destination = new ByteArrayOutputStream();
        try {
            content.writeTo(destination);
        } catch (final IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return destination.toString(StandardCharsets.UTF_8);
    }


    // Campo a campo, e o conteúdo **lido**: o `Resource` guarda uma fonte, não um arranjo, então o
    // `equals` dele não compara bytes — comparar duas fontes exigiria abrir e ler as duas. Afirmar
    // assim diz a mesma coisa, e de passagem prova que a fonte entrega o que foi guardado.
    private static void assertSameResource(final Resource expected, final Resource actual) throws IOException {
        assertEquals(expected.checksum(), actual.checksum());
        assertEquals(expected.contentType(), actual.contentType());
        assertEquals(expected.name(), actual.name());
        try (var expectedContent = expected.content().open();
                var actualContent = actual.content().open()) {
            assertArrayEquals(expectedContent.readAllBytes(), actualContent.readAllBytes());
        }
    }

}
