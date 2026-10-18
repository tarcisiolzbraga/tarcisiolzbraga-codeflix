package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaSource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// O que prova que o envio sobe **em fluxo**, e não montado em memória.
//
// O cliente do S3 é dublado para o `RequestBody` poder ser inspecionado: contra o armazenamento de
// verdade, as duas implementações gravariam o mesmo objeto e o teste não distinguiria nada — foi o
// erro que eu cometi na primeira versão da prova do download.
class S3StorageServiceUploadStreamingTest {

    private static final String CONTENT = "conteudo do arquivo";

    private final S3Client client = mock(S3Client.class);
    private final StorageService storageService = new S3StorageService(this.client, "bucket");

    // **O tamanho é obrigatório, e vem da fonte.** Sem `Content-Length` o SDK bufferiza o corpo para
    // descobri-lo, o que anularia a troca inteira.
    @Test
    void givenAFileToStore_whenStored_thenTheBodyCarriesTheLengthFromTheSource() {
        this.storageService.store("qualquer", resource());

        assertEquals(CONTENT.length(), capturedBody().optionalContentLength().orElseThrow());
    }

    // A fonte é aberta **durante** o envio, e não antes: o `RequestBody` guarda como abrir, não os
    // bytes.
    @Test
    void givenAFileToStore_whenStored_thenTheSourceIsOpenedByTheRequestBodyAndNotBefore() {
        final var opens = new int[1];
        final var source = new MediaSource() {
            @Override
            public InputStream open() {
                opens[0]++;
                return new ByteArrayInputStream(CONTENT.getBytes());
            }

            @Override
            public long length() {
                return CONTENT.length();
            }
        };

        this.storageService.store("qualquer", Resource.with(source, "abc1", "video/mp4", "duna.mp4"));

        // Uma vez: a do envio. A passada do checksum acontece na rota, antes de chegar aqui.
        assertEquals(1, opens[0]);
    }

    @Test
    void givenAFileToStore_whenStored_thenTheContentIsTheOneFromTheSource() throws Exception {
        this.storageService.store("qualquer", resource());

        try (var sent = capturedBody().contentStreamProvider().newStream()) {
            assertEquals(CONTENT, new String(sent.readAllBytes()));
        }
    }

    // O checksum e o nome continuam indo como metadado do objeto: é de lá que o download os lê de
    // volta, e perder isso quebraria o `get` sem o upload acusar nada.
    @Test
    void givenAFileToStore_whenStored_thenTheChecksumAndTheNameGoAsMetadata() {
        this.storageService.store("qualquer", resource());

        final var request = ArgumentCaptor.forClass(Consumer.class);
        verify(this.client).putObject(request.capture(), any(RequestBody.class));
        final var builder = PutObjectRequest.builder();
        request.getValue().accept(builder);
        final var built = builder.build();
        assertEquals("abc1", built.metadata().get("checksum"));
        assertEquals("duna.mp4", built.metadata().get("name"));
        assertEquals("video/mp4", built.contentType());
        assertTrue(built.key().equals("qualquer"));
    }

    private Resource resource() {
        return Resource.with(CONTENT.getBytes(), "abc1", "video/mp4", "duna.mp4");
    }

    private RequestBody capturedBody() {
        final var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(this.client).putObject(any(Consumer.class), body.capture());
        return body.getValue();
    }
}
