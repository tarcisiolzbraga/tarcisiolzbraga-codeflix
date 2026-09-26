package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

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

import com.tarcisiolzbraga.codeflix.admin.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.get.GetCastMemberByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
import java.time.Instant;
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
}
