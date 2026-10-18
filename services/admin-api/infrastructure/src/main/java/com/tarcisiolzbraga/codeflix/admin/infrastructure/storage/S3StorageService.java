package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.ResourceSlice;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

// Fala S3, então serve tanto ao Garage do desenvolvimento quanto a um bucket de verdade. Checksum e
// nome original viajam como metadados do objeto: é o que permite devolver o Resource como entrou.
public class S3StorageService implements StorageService {

    private static final String CHECKSUM_METADATA = "checksum";
    private static final String NAME_METADATA = "name";

    private final S3Client client;
    private final String bucket;

    public S3StorageService(final S3Client client, final String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void store(final String name, final Resource resource) {
        this.client.putObject(
                request -> request.bucket(this.bucket)
                        .key(name)
                        .contentType(resource.contentType())
                        .metadata(Map.of(
                                CHECKSUM_METADATA, resource.checksum(), NAME_METADATA, resource.name())),
                RequestBody.fromBytes(resource.content()));
    }

    private static final int RANGE_NOT_SATISFIABLE = 416;

    @Override
    public Optional<Resource> get(final String name) {
        try {
            final var object = this.client.getObjectAsBytes(
                    request -> request.bucket(this.bucket).key(name));
            final var response = object.response();
            return Optional.of(Resource.with(
                    object.asByteArray(),
                    response.metadata().get(CHECKSUM_METADATA),
                    response.contentType(),
                    response.metadata().get(NAME_METADATA)));
        } catch (final NoSuchKeyException _) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(final Set<String> names) {
        if (names.isEmpty()) {
            return;
        }
        final var objects = names.stream()
                .map(name -> ObjectIdentifier.builder().key(name).build())
                .toList();
        this.client.deleteObjects(request ->
                request.bucket(this.bucket).delete(Delete.builder().objects(objects).build()));
    }

    // O trecho, pedido ao próprio armazenamento.
    //
    // O cabeçalho vai **como veio**: o S3 aceita o formato `bytes=0-1023` e devolve o `Content-Range`
    // já calculado, com o intervalo servido e o tamanho total. Interpretar o cabeçalho aqui seria
    // repetir essa conta e errar nas pontas — e o acervo precisa justamente do Content-Range para
    // responder 206.
    @Override
    public Optional<ResourceSlice> getSlice(final String name, final String range) {
        try {
            // `getObject`, e não `getObjectAsBytes`: o primeiro devolve o corpo como fluxo, e o
            // segundo lê o trecho inteiro para a memória antes de qualquer coisa. O 416 e o 404
            // chegam **aqui**, na abertura, e não durante a escrita — então a rota ainda pode mudar
            // o status antes de começar a responder.
            final var object = this.client.getObject(request -> request.bucket(this.bucket).key(name).range(range));
            final var response = object.response();
            return Optional.of(new ResourceSlice(
                    // Quem abriu fecha: o `try-with-resources` garante que o fluxo do SDK não fica
                    // pendurado, inclusive quando a escrita falha no meio — um cliente que desiste
                    // do vídeo é caso comum, não exceção.
                    destination -> {
                        try (var content = object) {
                            content.transferTo(destination);
                        }
                    },
                    response.contentType(),
                    response.metadata().get(NAME_METADATA),
                    response.contentRange()));
        } catch (final NoSuchKeyException _) {
            return Optional.empty();
        } catch (final S3Exception refused) {
            // 416 é o armazenamento dizendo que o trecho não existe no arquivo. Virar Optional vazio
            // o transformaria em 404, que mentiria: o arquivo está lá.
            if (refused.statusCode() == RANGE_NOT_SATISFIABLE) {
                throw new RangeNotSatisfiableException(range, refused);
            }
            throw refused;
        }
    }
}
