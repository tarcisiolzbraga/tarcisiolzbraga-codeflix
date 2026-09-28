package com.tarcisiolzbraga.codeflix.admin.infrastructure.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class MediaResourceStorageGatewayIT {

    @Autowired
    private MediaResourceGateway mediaResourceGateway;

    @Test
    void givenAnAudioVideoResource_whenStore_thenReceiveItPendingAndAddressedByVideoAndType() {
        final var id = VideoID.unique();

        final var actualMedia =
                this.mediaResourceGateway.storeAudioVideo(id, videoResource(VideoMediaType.TRAILER));

        assertEquals("%s/TRAILER".formatted(id.getValue()), actualMedia.rawLocation());
        assertEquals("abc123", actualMedia.checksum());
        assertEquals("duna.mp4", actualMedia.name());
        assertEquals(MediaStatus.PENDING, actualMedia.status());
    }

    @Test
    void givenAnImageResource_whenStore_thenReceiveItAddressedByVideoAndType() {
        final var id = VideoID.unique();

        final var actualMedia = this.mediaResourceGateway.storeImage(id, videoResource(VideoMediaType.BANNER));

        assertEquals("%s/BANNER".formatted(id.getValue()), actualMedia.location());
    }

    @Test
    void givenAStoredResource_whenGetResource_thenReceiveItBack() {
        final var id = VideoID.unique();
        final var expectedResource = resource();
        this.mediaResourceGateway.storeAudioVideo(id, VideoResource.with(VideoMediaType.VIDEO, expectedResource));

        final var actualResource = this.mediaResourceGateway.getResource(id, VideoMediaType.VIDEO);

        assertEquals(Optional.of(expectedResource), actualResource);
    }

    @Test
    void givenNothingStored_whenGetResource_thenReceiveEmpty() {
        final var actualResource = this.mediaResourceGateway.getResource(VideoID.unique(), VideoMediaType.VIDEO);

        assertTrue(actualResource.isEmpty());
    }

    @Test
    void givenSeveralStoredResources_whenClearResources_thenRemoveAllOfThem() {
        final var id = VideoID.unique();
        this.mediaResourceGateway.storeAudioVideo(id, videoResource(VideoMediaType.VIDEO));
        this.mediaResourceGateway.storeImage(id, videoResource(VideoMediaType.THUMBNAIL_HALF));

        this.mediaResourceGateway.clearResources(id);

        assertTrue(this.mediaResourceGateway.getResource(id, VideoMediaType.VIDEO).isEmpty());
        assertTrue(this.mediaResourceGateway.getResource(id, VideoMediaType.THUMBNAIL_HALF).isEmpty());
    }

    @Test
    void givenTwoVideos_whenClearResourcesOfOne_thenKeepTheOther() {
        final var kept = VideoID.unique();
        final var removed = VideoID.unique();
        this.mediaResourceGateway.storeAudioVideo(kept, videoResource(VideoMediaType.VIDEO));
        this.mediaResourceGateway.storeAudioVideo(removed, videoResource(VideoMediaType.VIDEO));

        this.mediaResourceGateway.clearResources(removed);

        assertTrue(this.mediaResourceGateway.getResource(kept, VideoMediaType.VIDEO).isPresent());
    }

    private VideoResource videoResource(final VideoMediaType type) {
        return VideoResource.with(type, resource());
    }

    private Resource resource() {
        return Resource.with("conteudo do arquivo".getBytes(), "abc123", "video/mp4", "duna.mp4");
    }
}
