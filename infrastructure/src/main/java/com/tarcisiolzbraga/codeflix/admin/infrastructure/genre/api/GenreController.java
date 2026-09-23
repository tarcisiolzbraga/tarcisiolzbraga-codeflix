package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreCommand;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenreController implements GenreAPI {

    private static final String RESOURCE_PATH = "/genres/";

    private final CreateGenreUseCase createGenreUseCase;

    public GenreController(final CreateGenreUseCase createGenreUseCase) {
        this.createGenreUseCase = createGenreUseCase;
    }

    @Override
    public ResponseEntity<Object> create(final CreateGenreRequest request) {
        final var command = CreateGenreCommand.with(request.name(), request.isActive(), request.categories());
        return this.createGenreUseCase.execute(command).fold(this::unprocessableContent, this::created);
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateGenreOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id())).body(CreateGenreResponse.from(output));
    }
}
