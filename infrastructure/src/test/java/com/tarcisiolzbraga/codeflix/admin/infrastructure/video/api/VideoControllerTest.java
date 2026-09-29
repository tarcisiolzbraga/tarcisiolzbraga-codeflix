package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

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

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoMediaOutputs;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;
import com.tarcisiolzbraga.codeflix.admin.application.video.activate.ActivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.close.CloseVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.deactivate.DeactivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.delete.DeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.get.GetVideoByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.list.VideoListOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.open.OpenVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.publish.PublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.unpublish.UnpublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Os casos de uso são mockados um por um, e os records de agrupamento são os de verdade: assim o
// teste ainda passa pela ligação que o controller usa.
@ControllerTest(controllers = VideoController.class)
class VideoControllerTest {

    private static final String VIDEOS_PATH = "/videos";
    private static final String EXPECTED_ID = "123";
    private static final String EXPECTED_TITLE = "Duna";
    private static final String CATEGORY_ID = "c1";
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T08:00:00Z");
    private static final String VALID_BODY =
            """
            {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"12",
             "categories":["c1"]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateVideoUseCase createVideoUseCase;

    @MockitoBean
    private GetVideoByIdUseCase getVideoByIdUseCase;

    @MockitoBean
    private ListVideosUseCase listVideosUseCase;

    @MockitoBean
    private UpdateVideoUseCase updateVideoUseCase;

    @MockitoBean
    private DeleteVideoUseCase deleteVideoUseCase;

    @MockitoBean
    private PublishVideoUseCase publishVideoUseCase;

    @MockitoBean
    private UnpublishVideoUseCase unpublishVideoUseCase;

    @MockitoBean
    private OpenVideoUseCase openVideoUseCase;

    @MockitoBean
    private CloseVideoUseCase closeVideoUseCase;

    @MockitoBean
    private ActivateVideoUseCase activateVideoUseCase;

    @MockitoBean
    private DeactivateVideoUseCase deactivateVideoUseCase;

    @Test
    void givenValidBody_whenCallCreate_thenReturn201WithLocation() throws Exception {
        when(createVideoUseCase.execute(any())).thenReturn(Right(new CreateVideoOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(
                post(VIDEOS_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", VIDEOS_PATH + "/" + EXPECTED_ID))
                .andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(createVideoUseCase).execute(argThat(command -> EXPECTED_TITLE.equals(command.fields().title())
                && command.fields().launchedAt() == 2021
                && "12".equals(command.fields().rating())
                && command.references().categories().equals(Set.of(CATEGORY_ID))));
    }

    @Test
    void givenInvalidBody_whenCallCreate_thenReturn422WithTheErrors() throws Exception {
        final var notification = Notification.create();
        notification.append(new ValidationError("'rating' should not be null"));
        when(createVideoUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(post(VIDEOS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"99"}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0]").value("'rating' should not be null"));
    }

    @Test
    void givenExistingId_whenCallGetById_thenReturn200WithEveryField() throws Exception {
        when(getVideoByIdUseCase.execute(EXPECTED_ID)).thenReturn(outputWith(false));

        final var response = this.mockMvc.perform(get(VIDEOS_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXPECTED_ID))
                .andExpect(jsonPath("$.title").value(EXPECTED_TITLE))
                .andExpect(jsonPath("$.rating").value("12"))
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.categories[0]").value(CATEGORY_ID))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));
    }

    @Test
    void givenUnknownId_whenCallGetById_thenReturn404() throws Exception {
        when(getVideoByIdUseCase.execute(EXPECTED_ID))
                .thenThrow(NotFoundException.with(Video.class, VideoID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(get(VIDEOS_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Video with ID " + EXPECTED_ID + " was not found"));
    }

    @Test
    void givenSearchParams_whenCallList_thenPassThemToTheUseCase() throws Exception {
        final var item = new VideoListOutput(EXPECTED_ID, EXPECTED_TITLE, 2021, true, true, CREATED_AT);
        when(listVideosUseCase.execute(any())).thenReturn(new Pagination<>(1, 2, 3, List.of(item)));

        final var response = this.mockMvc.perform(get(VIDEOS_PATH)
                .param("search", "duna")
                .param("page", "1")
                .param("perPage", "2")
                .param("categories", CATEGORY_ID));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].title").value(EXPECTED_TITLE));
        verify(listVideosUseCase).execute(argThat(query -> "duna".equals(query.page().terms())
                && query.page().page() == 1
                && query.categories().size() == 1));
    }

    @Test
    void givenValidBody_whenCallUpdate_thenReturn200WithTheId() throws Exception {
        when(updateVideoUseCase.execute(any())).thenReturn(Right(new UpdateVideoOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(VIDEOS_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(updateVideoUseCase).execute(argThat(command -> EXPECTED_ID.equals(command.id())
                && EXPECTED_TITLE.equals(command.fields().title())));
    }

    @Test
    void givenExistingId_whenCallPublish_thenReturn200WithTheVideoPublished() throws Exception {
        when(publishVideoUseCase.execute(EXPECTED_ID)).thenReturn(outputWith(true));

        final var response = this.mockMvc.perform(put(VIDEOS_PATH + "/" + EXPECTED_ID + "/publish"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true));
        verify(publishVideoUseCase).execute(EXPECTED_ID);
    }

    @Test
    void givenExistingId_whenCallUnpublish_thenReturn200WithTheVideoUnpublished() throws Exception {
        when(unpublishVideoUseCase.execute(EXPECTED_ID)).thenReturn(outputWith(false));

        final var response = this.mockMvc.perform(put(VIDEOS_PATH + "/" + EXPECTED_ID + "/unpublish"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));
        verify(unpublishVideoUseCase).execute(EXPECTED_ID);
    }

    @Test
    void givenExistingId_whenCallOpen_thenReturn200AndCallTheUseCase() throws Exception {
        when(openVideoUseCase.execute(EXPECTED_ID)).thenReturn(outputWith(false));

        final var response = this.mockMvc.perform(put(VIDEOS_PATH + "/" + EXPECTED_ID + "/open"));

        response.andExpect(status().isOk());
        verify(openVideoUseCase).execute(EXPECTED_ID);
    }

    @Test
    void givenAnyId_whenCallDelete_thenReturn204AndCallTheUseCase() throws Exception {
        final var response = this.mockMvc.perform(delete(VIDEOS_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNoContent());
        verify(deleteVideoUseCase).execute(EXPECTED_ID);
    }

    private VideoOutput outputWith(final boolean published) {
        return new VideoOutput(
                EXPECTED_ID,
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2021, 155.0, "12"),
                new VideoReferenceIds(Set.of(CATEGORY_ID), Set.of(), Set.of()),
                noMedias(),
                false,
                published,
                true,
                CREATED_AT,
                UPDATED_AT);
    }

    @TestConfiguration
    static class UseCaseGroups {

        @Bean
        VideoUseCases videoUseCases(
                final CreateVideoUseCase create,
                final GetVideoByIdUseCase getById,
                final ListVideosUseCase list,
                final UpdateVideoUseCase update,
                final DeleteVideoUseCase delete) {
            return new VideoUseCases(create, getById, list, update, delete);
        }

        @Bean
        VideoStateUseCases videoStateUseCases(
                final PublishVideoUseCase publish,
                final UnpublishVideoUseCase unpublish,
                final OpenVideoUseCase open,
                final CloseVideoUseCase close,
                final ActivateVideoUseCase activate,
                final DeactivateVideoUseCase deactivate) {
            return new VideoStateUseCases(publish, unpublish, open, close, activate, deactivate);
        }
    }

    private VideoMediaOutputs noMedias() {
        return new VideoMediaOutputs(null, null, null, null, null);
    }

}
