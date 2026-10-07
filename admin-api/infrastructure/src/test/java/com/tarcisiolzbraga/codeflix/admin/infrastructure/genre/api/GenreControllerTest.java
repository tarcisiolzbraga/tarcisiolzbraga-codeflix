package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.activate.ActivateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.deactivate.DeactivateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.delete.DeleteGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.get.GetGenreByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.list.GenreListOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = GenreController.class)
class GenreControllerTest {

    private static final String GENRES_PATH = "/genres";
    private static final String EXPECTED_ID = "11111111-1111-1111-1111-111111111111";
    private static final String EXPECTED_NAME = "Ação";
    private static final Set<String> EXPECTED_CATEGORIES = Set.of("c1", "c2");
    private static final String VALID_BODY =
            """
            {"name":"Ação","categories":["c1","c2"],"active":false}""";
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGenreUseCase createGenreUseCase;

    @MockitoBean
    private GetGenreByIdUseCase getGenreByIdUseCase;

    @MockitoBean
    private ListGenresUseCase listGenresUseCase;

    @MockitoBean
    private UpdateGenreUseCase updateGenreUseCase;

    @MockitoBean
    private ActivateGenreUseCase activateGenreUseCase;

    @MockitoBean
    private DeactivateGenreUseCase deactivateGenreUseCase;

    @MockitoBean
    private DeleteGenreUseCase deleteGenreUseCase;

    @Test
    void givenValidBody_whenCallCreate_thenReturn201WithLocation() throws Exception {
        when(createGenreUseCase.execute(any())).thenReturn(Right(new CreateGenreOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(
                post(GENRES_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", GENRES_PATH + "/" + EXPECTED_ID))
                .andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(createGenreUseCase).execute(argThat(command -> EXPECTED_NAME.equals(command.name())
                && EXPECTED_CATEGORIES.equals(command.categories())
                && !command.isActive()));
    }

    @Test
    void givenBodyWithOnlyName_whenCallCreate_thenCreateItActiveWithoutCategories() throws Exception {
        when(createGenreUseCase.execute(any())).thenReturn(Right(new CreateGenreOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(post(GENRES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ação"}"""));

        response.andExpect(status().isCreated());
        verify(createGenreUseCase).execute(argThat(command -> command.isActive() && command.categories().isEmpty()));
    }

    @Test
    void givenInvalidNameAndUnknownCategory_whenCallCreate_thenReturn422WithEveryError() throws Exception {
        final var notification = Notification.create(new ValidationError("Some categories could not be found: c9"))
                .append(new ValidationError("'name' should not be null"));
        when(createGenreUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(post(GENRES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categories":["c9"]}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Some categories could not be found: c9"))
                .andExpect(jsonPath("$.errors[0]").value("Some categories could not be found: c9"))
                .andExpect(jsonPath("$.errors[1]").value("'name' should not be null"));
    }

    @Test
    void givenValidId_whenCallGetById_thenReturn200WithTheGenreAndSortedCategories() throws Exception {
        final var output =
                new GenreOutput(EXPECTED_ID, EXPECTED_NAME, true, Set.of("c2", "c1"), CREATED_AT, UPDATED_AT);
        when(getGenreByIdUseCase.execute(EXPECTED_ID)).thenReturn(output);

        final var response = this.mockMvc.perform(get(GENRES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXPECTED_ID))
                .andExpect(jsonPath("$.name").value(EXPECTED_NAME))
                .andExpect(jsonPath("$.categories[0]").value("c1"))
                .andExpect(jsonPath("$.categories[1]").value("c2"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    void givenUnknownId_whenCallGetById_thenReturn404() throws Exception {
        when(getGenreByIdUseCase.execute(EXPECTED_ID))
                .thenThrow(NotFoundException.with(Genre.class, GenreID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(get(GENRES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Genre with ID %s was not found".formatted(EXPECTED_ID)));
    }

    @Test
    void givenSearchParams_whenCallList_thenReturn200WithPagination() throws Exception {
        final var item = new GenreListOutput(EXPECTED_ID, EXPECTED_NAME, true, Set.of("c2", "c1"), CREATED_AT);
        when(listGenresUseCase.execute(new SearchQuery(1, 5, "aca", "createdAt", "desc")))
                .thenReturn(new Pagination<>(1, 5, 1L, List.of(item)));

        final var response = this.mockMvc.perform(get(GENRES_PATH)
                .queryParam("search", "aca")
                .queryParam("page", "1")
                .queryParam("perPage", "5")
                .queryParam("sort", "createdAt")
                .queryParam("dir", "desc"));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value(EXPECTED_NAME))
                .andExpect(jsonPath("$.items[0].categories[0]").value("c1"));
    }

    @Test
    void givenNoSearchParam_whenCallList_thenUseTheDefaults() throws Exception {
        when(listGenresUseCase.execute(new SearchQuery(0, 10, null, "name", "asc")))
                .thenReturn(new Pagination<>(0, 10, 0L, List.of()));

        final var response = this.mockMvc.perform(get(GENRES_PATH));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.perPage").value(10));
    }

    @Test
    void givenValidBody_whenCallUpdate_thenReturn200() throws Exception {
        when(updateGenreUseCase.execute(any())).thenReturn(Right(new UpdateGenreOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ação","categories":["c1","c2"]}"""));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(updateGenreUseCase).execute(argThat(command -> EXPECTED_ID.equals(command.id())
                && EXPECTED_NAME.equals(command.name())
                && EXPECTED_CATEGORIES.equals(command.categories())));
    }

    @Test
    void givenInvalidName_whenCallUpdate_thenReturn422WithErrors() throws Exception {
        final var notification = Notification.create(new ValidationError("'name' should not be empty"));
        when(updateGenreUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"   "}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0]").value("'name' should not be empty"));
    }

    @Test
    void givenUnknownId_whenCallUpdate_thenReturn404() throws Exception {
        when(updateGenreUseCase.execute(any()))
                .thenThrow(NotFoundException.with(Genre.class, GenreID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY));

        response.andExpect(status().isNotFound());
    }

    @Test
    void givenValidId_whenCallActivate_thenReturn200WithTheActiveGenre() throws Exception {
        final var output =
                new GenreOutput(EXPECTED_ID, EXPECTED_NAME, true, EXPECTED_CATEGORIES, CREATED_AT, UPDATED_AT);
        when(activateGenreUseCase.execute(EXPECTED_ID)).thenReturn(output);

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID + "/activate"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void givenUnknownId_whenCallActivate_thenReturn404() throws Exception {
        when(activateGenreUseCase.execute(EXPECTED_ID))
                .thenThrow(NotFoundException.with(Genre.class, GenreID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID + "/activate"));

        response.andExpect(status().isNotFound());
    }

    @Test
    void givenValidId_whenCallDeactivate_thenReturn200WithTheInactiveGenre() throws Exception {
        final var output =
                new GenreOutput(EXPECTED_ID, EXPECTED_NAME, false, EXPECTED_CATEGORIES, CREATED_AT, UPDATED_AT);
        when(deactivateGenreUseCase.execute(EXPECTED_ID)).thenReturn(output);

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID + "/deactivate"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void givenUnknownId_whenCallDeactivate_thenReturn404() throws Exception {
        when(deactivateGenreUseCase.execute(EXPECTED_ID))
                .thenThrow(NotFoundException.with(Genre.class, GenreID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(GENRES_PATH + "/" + EXPECTED_ID + "/deactivate"));

        response.andExpect(status().isNotFound());
    }

    @Test
    void givenValidId_whenCallDelete_thenReturn204() throws Exception {
        final var response = this.mockMvc.perform(delete(GENRES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNoContent());
        verify(deleteGenreUseCase).execute(EXPECTED_ID);
    }
}
