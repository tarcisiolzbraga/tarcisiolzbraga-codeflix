package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import com.tarcisiolzbraga.codeflix.admin.application.genre.activate.ActivateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreCommand;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.deactivate.DeactivateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.delete.DeleteGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.get.GetGenreByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreCommand;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreSearchRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.UpdateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.UpdateGenreResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenreController implements GenreAPI {

    private static final String RESOURCE_PATH = "/genres/";

    private final CreateGenreUseCase createGenreUseCase;
    private final GetGenreByIdUseCase getGenreByIdUseCase;
    private final ListGenresUseCase listGenresUseCase;
    private final UpdateGenreUseCase updateGenreUseCase;
    private final ActivateGenreUseCase activateGenreUseCase;
    private final DeactivateGenreUseCase deactivateGenreUseCase;
    private final DeleteGenreUseCase deleteGenreUseCase;

    public GenreController(
            final CreateGenreUseCase createGenreUseCase,
            final GetGenreByIdUseCase getGenreByIdUseCase,
            final ListGenresUseCase listGenresUseCase,
            final UpdateGenreUseCase updateGenreUseCase,
            final ActivateGenreUseCase activateGenreUseCase,
            final DeactivateGenreUseCase deactivateGenreUseCase,
            final DeleteGenreUseCase deleteGenreUseCase) {
        this.createGenreUseCase = createGenreUseCase;
        this.getGenreByIdUseCase = getGenreByIdUseCase;
        this.listGenresUseCase = listGenresUseCase;
        this.updateGenreUseCase = updateGenreUseCase;
        this.activateGenreUseCase = activateGenreUseCase;
        this.deactivateGenreUseCase = deactivateGenreUseCase;
        this.deleteGenreUseCase = deleteGenreUseCase;
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

    @Override
    public ResponseEntity<Object> update(final String id, final UpdateGenreRequest request) {
        final var command = UpdateGenreCommand.with(id, request.name(), request.categories());
        return this.updateGenreUseCase.execute(command).fold(this::unprocessableContent, this::updated);
    }

    @Override
    public GenreResponse activate(final String id) {
        return GenreResponse.from(this.activateGenreUseCase.execute(id));
    }

    @Override
    public GenreResponse deactivate(final String id) {
        return GenreResponse.from(this.deactivateGenreUseCase.execute(id));
    }

    @Override
    public void deleteById(final String id) {
        this.deleteGenreUseCase.execute(id);
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateGenreOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id())).body(CreateGenreResponse.from(output));
    }

    private ResponseEntity<Object> updated(final UpdateGenreOutput output) {
        return ResponseEntity.ok(UpdateGenreResponse.from(output));
    }
}
