package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.get.GetMediaCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.upload.UploadMediaCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.upload.UploadMediaOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UploadMediaResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoSearchRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.util.Hashing;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VideoController implements VideoAPI {

    private static final String RESOURCE_PATH = "/videos/";
    private static final String MEDIA_PATH = "/medias/";
    private static final String FALLBACK_CONTENT_TYPE = "application/octet-stream";
    private static final String CONTENT_DISPOSITION = "attachment; filename=\"%s\"";

    private final VideoUseCases useCases;
    private final VideoStateUseCases stateUseCases;
    private final VideoMediaUseCases mediaUseCases;

    public VideoController(
            final VideoUseCases useCases,
            final VideoStateUseCases stateUseCases,
            final VideoMediaUseCases mediaUseCases) {
        this.useCases = useCases;
        this.stateUseCases = stateUseCases;
        this.mediaUseCases = mediaUseCases;
    }

    @Override
    public ResponseEntity<Object> create(final CreateVideoRequest request) {
        final var command = CreateVideoCommand.with(request.toFields(), request.toReferences());
        return this.useCases.create().execute(command).fold(this::unprocessableContent, this::created);
    }

    @Override
    public Pagination<VideoListResponse> list(final VideoSearchRequest request) {
        return this.useCases.list().execute(request.toSearchQuery()).map(VideoListResponse::from);
    }

    @Override
    public VideoResponse getById(final String id) {
        return VideoResponse.from(this.useCases.getById().execute(id));
    }

    @Override
    public ResponseEntity<Object> update(final String id, final UpdateVideoRequest request) {
        final var command = UpdateVideoCommand.with(id, request.toFields(), request.toReferences());
        return this.useCases.update().execute(command).fold(this::unprocessableContent, this::updated);
    }

    @Override
    public VideoResponse publish(final String id) {
        return VideoResponse.from(this.stateUseCases.publish().execute(id));
    }

    @Override
    public VideoResponse unpublish(final String id) {
        return VideoResponse.from(this.stateUseCases.unpublish().execute(id));
    }

    @Override
    public VideoResponse open(final String id) {
        return VideoResponse.from(this.stateUseCases.open().execute(id));
    }

    @Override
    public VideoResponse close(final String id) {
        return VideoResponse.from(this.stateUseCases.close().execute(id));
    }

    @Override
    public VideoResponse activate(final String id) {
        return VideoResponse.from(this.stateUseCases.activate().execute(id));
    }

    @Override
    public VideoResponse deactivate(final String id) {
        return VideoResponse.from(this.stateUseCases.deactivate().execute(id));
    }

    @Override
    public ResponseEntity<UploadMediaResponse> uploadMedia(
            final String id, final VideoMediaType type, final MultipartFile file) {
        final var command = new UploadMediaCommand(id, VideoResource.with(type, resourceOf(file)));
        return uploaded(this.mediaUseCases.upload().execute(command));
    }

    @Override
    public ResponseEntity<byte[]> getMedia(final String id, final VideoMediaType type) {
        final var media = this.mediaUseCases.getMedia().execute(new GetMediaCommand(id, type));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION.formatted(media.name()))
                .body(media.content());
    }

    @Override
    public void deleteById(final String id) {
        this.useCases.delete().execute(id);
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateVideoOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id())).body(CreateVideoResponse.from(output));
    }

    private ResponseEntity<Object> updated(final UpdateVideoOutput output) {
        return ResponseEntity.ok(UpdateVideoResponse.from(output));
    }

    private ResponseEntity<UploadMediaResponse> uploaded(final UploadMediaOutput output) {
        final var location = RESOURCE_PATH + output.videoId() + MEDIA_PATH + output.type().name();
        return ResponseEntity.created(URI.create(location)).body(UploadMediaResponse.from(output));
    }

    // O tipo de conteúdo e o nome vêm do envio e são guardados junto do arquivo, para a rota de
    // download devolver o arquivo como ele entrou.
    private Resource resourceOf(final MultipartFile file) {
        try {
            final var content = file.getBytes();
            final var contentType = file.getContentType() == null ? FALLBACK_CONTENT_TYPE : file.getContentType();
            final var name = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
            return Resource.with(content, Hashing.checksumOf(content), contentType, name);
        } catch (final IOException exception) {
            throw new IllegalStateException("could not read the uploaded file", exception);
        }
    }
}
