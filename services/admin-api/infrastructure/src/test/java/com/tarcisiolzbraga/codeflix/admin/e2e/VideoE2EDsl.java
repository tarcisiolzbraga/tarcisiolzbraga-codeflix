package com.tarcisiolzbraga.codeflix.admin.e2e;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UploadMediaResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoResponse;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP, sem atalho pelo gateway ou pelo banco.
public interface VideoE2EDsl {

    String VIDEOS_PATH = "/videos";
    String DEFAULT_DESCRIPTION = "Uma sinopse qualquer";

    RestClient client();

    default String givenAVideo(final String title, final String... categories) {
        return createAVideo(new CreateVideoRequest(
                        title, DEFAULT_DESCRIPTION, 2021, 155.0, "12", Set.of(categories), Set.of(), Set.of()))
                .id();
    }

    default CreateVideoResponse createAVideo(final CreateVideoRequest request) {
        return client().post()
                .uri(VIDEOS_PATH)
                .body(request)
                .retrieve()
                .body(CreateVideoResponse.class);
    }

    default VideoResponse retrieveAVideo(final String id) {
        return client().get().uri(VIDEOS_PATH + "/{id}", id).retrieve().body(VideoResponse.class);
    }

    default Pagination<VideoListResponse> listVideos(final int page, final int perPage) {
        return listVideos(page, perPage, null);
    }

    default Pagination<VideoListResponse> listVideos(final int page, final int perPage, final String search) {
        return client().get()
                .uri(builder -> builder.path(VIDEOS_PATH)
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParamIfPresent("search", Optional.ofNullable(search))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    default Pagination<VideoListResponse> listVideosOfCategory(final String categoryId) {
        return client().get()
                .uri(builder -> builder.path(VIDEOS_PATH).queryParam("categories", categoryId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    default void updateAVideo(final String id, final String title, final String... categories) {
        client().put()
                .uri(VIDEOS_PATH + "/{id}", id)
                .body(new UpdateVideoRequest(
                        title, DEFAULT_DESCRIPTION, 2024, 166.0, "14", Set.of(categories), Set.of(), Set.of()))
                .retrieve()
                .toBodilessEntity();
    }

    default VideoResponse publishAVideo(final String id) {
        return changeState(id, "publish");
    }

    default VideoResponse unpublishAVideo(final String id) {
        return changeState(id, "unpublish");
    }

    default VideoResponse openAVideo(final String id) {
        return changeState(id, "open");
    }

    default VideoResponse deactivateAVideo(final String id) {
        return changeState(id, "deactivate");
    }

    default UploadMediaResponse uploadMedia(final String id, final String type, final E2EMediaFile file) {
        final var body = new MultipartBodyBuilder();
        body.part("file", new ByteArrayResource(file.content()))
                .filename(file.filename())
                .contentType(MediaType.parseMediaType(file.contentType()));
        return client().post()
                .uri(VIDEOS_PATH + "/{id}/medias/{type}", id, type)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(UploadMediaResponse.class);
    }

    default byte[] downloadMedia(final String id, final String type) {
        return client().get()
                .uri(VIDEOS_PATH + "/{id}/medias/{type}", id, type)
                .retrieve()
                .body(byte[].class);
    }

    default void deleteAVideo(final String id) {
        client().delete().uri(VIDEOS_PATH + "/{id}", id).retrieve().toBodilessEntity();
    }

    private VideoResponse changeState(final String id, final String action) {
        return client().put()
                .uri(VIDEOS_PATH + "/{id}/{action}", id, action)
                .retrieve()
                .body(VideoResponse.class);
    }
}
