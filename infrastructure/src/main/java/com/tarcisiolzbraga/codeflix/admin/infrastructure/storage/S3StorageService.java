package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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
}
