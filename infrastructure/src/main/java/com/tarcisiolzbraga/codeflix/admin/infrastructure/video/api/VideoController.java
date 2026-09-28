package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.UpdateVideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoSearchRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VideoController implements VideoAPI {

    private static final String RESOURCE_PATH = "/videos/";

    private final VideoUseCases useCases;
    private final VideoStateUseCases stateUseCases;

    public VideoController(final VideoUseCases useCases, final VideoStateUseCases stateUseCases) {
        this.useCases = useCases;
        this.stateUseCases = stateUseCases;
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
}
