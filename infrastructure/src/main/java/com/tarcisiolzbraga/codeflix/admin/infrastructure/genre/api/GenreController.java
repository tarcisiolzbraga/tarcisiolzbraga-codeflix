package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreCommand;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.get.GetGenreByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreSearchRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenreController implements GenreAPI {

    private static final String RESOURCE_PATH = "/genres/";

    private final CreateGenreUseCase createGenreUseCase;
    private final GetGenreByIdUseCase getGenreByIdUseCase;
    private final ListGenresUseCase listGenresUseCase;

    public GenreController(
            final CreateGenreUseCase createGenreUseCase,
            final GetGenreByIdUseCase getGenreByIdUseCase,
            final ListGenresUseCase listGenresUseCase) {
        this.createGenreUseCase = createGenreUseCase;
        this.getGenreByIdUseCase = getGenreByIdUseCase;
        this.listGenresUseCase = listGenresUseCase;
    }

    @Override
    public ResponseEntity<Object> create(final CreateGenreRequest request) {
        final var command = CreateGenreCommand.with(request.name(), request.isActive(), request.categories());
        return this.createGenreUseCase.execute(command).fold(this::unprocessableContent, this::created);
    }

    @Override
    public Pagination<GenreListResponse> list(final GenreSearchRequest request) {
        return this.listGenresUseCase.execute(request.toSearchQuery()).map(GenreListResponse::from);
    }

    @Override
    public GenreResponse getById(final String id) {
        return GenreResponse.from(this.getGenreByIdUseCase.execute(id));
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateGenreOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id())).body(CreateGenreResponse.from(output));
    }
}
