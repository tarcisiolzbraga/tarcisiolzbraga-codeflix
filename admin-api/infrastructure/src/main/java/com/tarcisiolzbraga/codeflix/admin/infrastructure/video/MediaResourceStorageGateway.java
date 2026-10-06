package com.tarcisiolzbraga.codeflix.admin.infrastructure.video;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.ImageMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.storage.StorageService;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// O par do VideoMySQLGateway para o arquivo: o agregado vai para o banco, o arquivo vai para o
// armazenamento. O nome do objeto é calculado do vídeo e do tipo, então apagar tudo de um vídeo
// não precisa listar o bucket.
@Component
public class MediaResourceStorageGateway implements MediaResourceGateway {

    private final StorageService storageService;

    public MediaResourceStorageGateway(final StorageService storageService) {
        this.storageService = Objects.requireNonNull(storageService, "'storageService' should not be null");
    }

    @Override
    public AudioVideoMedia storeAudioVideo(final VideoID id, final VideoResource resource) {
        final var location = locationOf(id, resource.type());
        this.storageService.store(location, resource.resource());
        return AudioVideoMedia.with(resource.resource().checksum(), resource.resource().name(), location);
    }

    @Override
    public ImageMedia storeImage(final VideoID id, final VideoResource resource) {
        final var location = locationOf(id, resource.type());
        this.storageService.store(location, resource.resource());
        return ImageMedia.with(resource.resource().checksum(), resource.resource().name(), location);
    }

    @Override
    public Optional<Resource> getResource(final VideoID id, final VideoMediaType type) {
        return this.storageService.get(locationOf(id, type));
    }

    @Override
    public void clearResources(final VideoID id) {
        this.storageService.delete(locationsOf(id));
    }

    private String locationOf(final VideoID id, final VideoMediaType type) {
        return "%s/%s".formatted(id.getValue(), type.name());
    }

    private Set<String> locationsOf(final VideoID id) {
        return Arrays.stream(VideoMediaType.values())
                .map(type -> locationOf(id, type))
                .collect(Collectors.toUnmodifiableSet());
    }
}
