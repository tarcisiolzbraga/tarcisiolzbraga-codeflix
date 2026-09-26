package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

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

import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
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
    private static final String VALID_BODY =
            """
            {"name":"Vin Diesel","type":"ACTOR","active":false}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCastMemberUseCase createCastMemberUseCase;

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
}
