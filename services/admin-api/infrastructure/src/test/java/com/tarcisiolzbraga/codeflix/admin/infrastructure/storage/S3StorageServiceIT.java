package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
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
    void givenAResource_whenStoreAndGet_thenReceiveItBackWholeWithItsMetadata() {
        final var name = uniqueName();
        final var expectedResource = resource();

        this.storageService.store(name, expectedResource);

        assertEquals(Optional.of(expectedResource), this.storageService.get(name));
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

    private String uniqueName() {
        return UUID.randomUUID().toString();
    }

    private Resource resource() {
        return Resource.with("conteudo do arquivo".getBytes(), "abc123", "video/mp4", "duna.mp4");
    }
}
