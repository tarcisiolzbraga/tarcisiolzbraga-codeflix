package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.get.GetGenreByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = GenreController.class)
class GenreControllerTest {

    private static final String GENRES_PATH = "/genres";
    private static final String EXPECTED_ID = "123";
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
                .andExpect(jsonPath("$.message").value("Genre with ID 123 was not found"));
    }
}
