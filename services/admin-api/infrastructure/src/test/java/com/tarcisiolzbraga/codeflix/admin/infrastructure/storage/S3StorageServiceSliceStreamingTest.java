package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaContent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

// O que prova que o trecho sai **em fluxo**, e não montado em memória.
//
// A primeira versão disto era um teste de integração que media duas coisas erradas: "nada foi lido"
// olhava o destino do teste, que obviamente estava vazio antes de alguém escrever nele; e "chegou em
// partes" vale para os dois casos, porque `transferTo` escreve em blocos de 8 KB mesmo a partir de um
// arranjo já carregado. Os dois passavam com o comportamento antigo — a conferência ao contrário
// pegou, e é para isso que ela existe.
//
// A propriedade que de fato mudou, e que este teste afirma, é a da costura: **`getSlice` não consome
// o corpo**, quem consome é o `writeTo`, e ele fecha o fluxo depois. O cliente do S3 é dublado para
// o teste poder contar as leituras — contra o armazenamento de verdade não há como observá-las.
class S3StorageServiceSliceStreamingTest {

    private static final String CONTENT = "conteudo do arquivo";
    // A mesma chave que o S3StorageService grava e lê.
    private static final String NAME_METADATA = "name";

    private final S3Client client = mock(S3Client.class);
    private final StorageService storageService = new S3StorageService(this.client, "bucket");

    // Os dois caminhos do SDK ficam armados: `getObject` devolve o fluxo que conta, e
    // `getObjectAsBytes` devolve os bytes prontos, como o SDK de verdade faria. Assim a falha com o
    // comportamento antigo é pela **propriedade**, e não por falta de dublê.
    private CountingInputStream armed() {
        final var counting = new CountingInputStream(new ByteArrayInputStream(CONTENT.getBytes()));
        final var response = GetObjectResponse.builder()
                .contentType("video/mp4")
                .contentRange("bytes 0-18/19")
                .metadata(Map.of(NAME_METADATA, "duna.mp4"))
                .build();
        when(this.client.getObject(any(Consumer.class)))
                .thenReturn(new ResponseInputStream<>(response, counting));
        when(this.client.getObjectAsBytes(any(Consumer.class)))
                .thenReturn(ResponseBytes.fromByteArray(response, CONTENT.getBytes()));
        return counting;
    }

    // Este afirma **qual chamada do SDK** é feita, e não só que nada chegou ao destino do teste. É
    // acoplamento à implementação de propósito: a escolha entre `getObject` e `getObjectAsBytes` **é**
    // o comportamento aqui, e não há outro jeito de observá-la — a primeira versão deste teste olhava
    // só o contador de leituras, que fica em zero nas duas implementações e por isso não provava nada.
    @Test
    void givenASliceRequest_whenItReturns_thenTheBodyIsFetchedLazilyAndNotReadYet() {
        final var counting = armed();

        final var slice = this.storageService.getSlice("qualquer", "bytes=0-18");

        assertTrue(slice.isPresent());
        verify(this.client).getObject(any(Consumer.class));
        verify(this.client, never()).getObjectAsBytes(any(Consumer.class));
        assertEquals(0, counting.reads, "o corpo foi lido antes de alguém pedir");
        assertFalse(counting.closed);
    }

    @Test
    void givenASlice_whenTheContentIsWritten_thenItIsReadFromTheStreamAndClosed() {
        final var counting = armed();
        final var slice = this.storageService.getSlice("qualquer", "bytes=0-18").orElseThrow();
        final var destination = new ByteArrayOutputStream();

        write(slice.content(), destination);

        assertTrue(counting.reads > 0, "o trecho não veio do fluxo");
        assertTrue(counting.closed, "o fluxo do SDK ficou aberto depois de escrever");
        assertEquals(CONTENT, destination.toString(StandardCharsets.UTF_8));
    }

    // Cliente que desiste do vídeo é caso comum, não exceção: o fluxo tem de fechar mesmo quando a
    // escrita falha no meio.
    @Test
    void givenADestinationThatFails_whenWriting_thenTheStreamIsStillClosed() {
        final var counting = armed();
        final var slice = this.storageService.getSlice("qualquer", "bytes=0-18").orElseThrow();

        try {
            slice.content().writeTo(new OutputStream() {
                @Override
                public void write(final int singleByte) throws IOException {
                    throw new IOException("o cliente desistiu");
                }

                @Override
                public void write(final byte[] bytes, final int offset, final int length) throws IOException {
                    throw new IOException("o cliente desistiu");
                }
            });
        } catch (final IOException expected) {
            // é o que o teste provoca
        }

        assertTrue(counting.closed, "o fluxo do SDK ficou aberto depois de a escrita falhar");
    }

    private static void write(final MediaContent content, final ByteArrayOutputStream destination) {
        try {
            content.writeTo(destination);
        } catch (final IOException failure) {
            throw new IllegalStateException("não foi possível escrever o trecho", failure);
        }
    }

    private static final class CountingInputStream extends FilterInputStream {

        private int reads;
        private boolean closed;

        private CountingInputStream(final ByteArrayInputStream source) {
            super(source);
        }

        @Override
        public int read() throws IOException {
            this.reads++;
            return super.read();
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            this.reads++;
            return super.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }

}
