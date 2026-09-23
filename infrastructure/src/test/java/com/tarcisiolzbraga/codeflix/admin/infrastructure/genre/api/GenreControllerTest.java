package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.api;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGenreUseCase createGenreUseCase;

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
}
