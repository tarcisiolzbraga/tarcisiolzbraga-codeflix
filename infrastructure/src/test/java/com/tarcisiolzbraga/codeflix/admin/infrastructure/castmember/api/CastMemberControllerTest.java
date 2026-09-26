package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.activate.ActivateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.deactivate.DeactivateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.get.GetCastMemberByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.CastMemberListOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.ListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = CastMemberController.class)
class CastMemberControllerTest {

    private static final String CAST_MEMBERS_PATH = "/cast-members";
    private static final String EXPECTED_ID = "123";
    private static final String EXPECTED_NAME = "Vin Diesel";
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T08:00:00Z");
    private static final String VALID_BODY =
            """
            {"name":"Vin Diesel","type":"ACTOR","active":false}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCastMemberUseCase createCastMemberUseCase;

    @MockitoBean
    private GetCastMemberByIdUseCase getCastMemberByIdUseCase;

    @MockitoBean
    private ListCastMembersUseCase listCastMembersUseCase;

    @MockitoBean
    private UpdateCastMemberUseCase updateCastMemberUseCase;

    @MockitoBean
    private ActivateCastMemberUseCase activateCastMemberUseCase;

    @MockitoBean
    private DeactivateCastMemberUseCase deactivateCastMemberUseCase;

    @Test
    void givenValidBody_whenCallCreate_thenReturn201WithLocation() throws Exception {
        when(createCastMemberUseCase.execute(any())).thenReturn(Right(new CreateCastMemberOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(
                post(CAST_MEMBERS_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", CAST_MEMBERS_PATH + "/" + EXPECTED_ID))
                .andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(createCastMemberUseCase).execute(argThat(command -> EXPECTED_NAME.equals(command.name())
                && "ACTOR".equals(command.type())
                && !command.isActive()));
    }

    @Test
    void givenInvalidBody_whenCallCreate_thenReturn422WithTheErrors() throws Exception {
        final var notification = Notification.create();
        notification.append(new ValidationError("'type' should be one of ACTOR, DIRECTOR"));
        when(createCastMemberUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(post(CAST_MEMBERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Vin Diesel","type":"SINGER"}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0]").value("'type' should be one of ACTOR, DIRECTOR"));
    }

    @Test
    void givenExistingId_whenCallGetById_thenReturn200WithEveryField() throws Exception {
        when(getCastMemberByIdUseCase.execute(EXPECTED_ID))
                .thenReturn(new CastMemberOutput(EXPECTED_ID, EXPECTED_NAME, "ACTOR", true, CREATED_AT, UPDATED_AT));

        final var response = this.mockMvc.perform(get(CAST_MEMBERS_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXPECTED_ID))
                .andExpect(jsonPath("$.name").value(EXPECTED_NAME))
                .andExpect(jsonPath("$.type").value("ACTOR"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    void givenUnknownId_whenCallGetById_thenReturn404() throws Exception {
        when(getCastMemberByIdUseCase.execute(EXPECTED_ID))
                .thenThrow(NotFoundException.with(CastMember.class, CastMemberID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(get(CAST_MEMBERS_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("CastMember with ID " + EXPECTED_ID + " was not found"));
    }

    @Test
    void givenSearchParams_whenCallList_thenPassThemToTheUseCaseAndReturnThePage() throws Exception {
        final var item = new CastMemberListOutput(EXPECTED_ID, EXPECTED_NAME, "ACTOR", true, CREATED_AT);
        when(listCastMembersUseCase.execute(any())).thenReturn(new Pagination<>(1, 2, 3, List.of(item)));

        final var response = this.mockMvc.perform(get(CAST_MEMBERS_PATH)
                .param("search", "vin")
                .param("page", "1")
                .param("perPage", "2")
                .param("sort", "createdAt")
                .param("dir", "desc"));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.perPage").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].id").value(EXPECTED_ID))
                .andExpect(jsonPath("$.items[0].type").value("ACTOR"));
        verify(listCastMembersUseCase).execute(argThat(query -> "vin".equals(query.terms())
                && query.page() == 1
                && query.perPage() == 2
                && "createdAt".equals(query.sort())
                && "desc".equals(query.direction())));
    }

    @Test
    void givenNoParam_whenCallList_thenUseTheDefaults() throws Exception {
        when(listCastMembersUseCase.execute(any())).thenReturn(new Pagination<>(0, 10, 0, List.of()));

        final var response = this.mockMvc.perform(get(CAST_MEMBERS_PATH));

        response.andExpect(status().isOk());
        verify(listCastMembersUseCase).execute(argThat(query -> query.page() == 0
                && query.perPage() == 10
                && "name".equals(query.sort())
                && "asc".equals(query.direction())));
    }

    @Test
    void givenValidBody_whenCallUpdate_thenReturn200WithTheId() throws Exception {
        when(updateCastMemberUseCase.execute(any())).thenReturn(Right(new UpdateCastMemberOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(CAST_MEMBERS_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Vin Diesel","type":"DIRECTOR"}"""));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(updateCastMemberUseCase).execute(argThat(command -> EXPECTED_ID.equals(command.id())
                && EXPECTED_NAME.equals(command.name())
                && "DIRECTOR".equals(command.type())));
    }

    @Test
    void givenInvalidBody_whenCallUpdate_thenReturn422WithTheErrors() throws Exception {
        final var notification = Notification.create();
        notification.append(new ValidationError("'name' should not be empty"));
        when(updateCastMemberUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(put(CAST_MEMBERS_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":" ","type":"ACTOR"}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0]").value("'name' should not be empty"));
    }

    @Test
    void givenExistingId_whenCallActivate_thenReturn200WithTheMemberActive() throws Exception {
        when(activateCastMemberUseCase.execute(EXPECTED_ID))
                .thenReturn(new CastMemberOutput(EXPECTED_ID, EXPECTED_NAME, "ACTOR", true, CREATED_AT, UPDATED_AT));

        final var response = this.mockMvc.perform(put(CAST_MEMBERS_PATH + "/" + EXPECTED_ID + "/activate"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        verify(activateCastMemberUseCase).execute(EXPECTED_ID);
    }

    @Test
    void givenExistingId_whenCallDeactivate_thenReturn200WithTheMemberInactive() throws Exception {
        when(deactivateCastMemberUseCase.execute(EXPECTED_ID))
                .thenReturn(new CastMemberOutput(EXPECTED_ID, EXPECTED_NAME, "ACTOR", false, CREATED_AT, UPDATED_AT));

        final var response = this.mockMvc.perform(put(CAST_MEMBERS_PATH + "/" + EXPECTED_ID + "/deactivate"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        verify(deactivateCastMemberUseCase).execute(EXPECTED_ID);
    }
}
