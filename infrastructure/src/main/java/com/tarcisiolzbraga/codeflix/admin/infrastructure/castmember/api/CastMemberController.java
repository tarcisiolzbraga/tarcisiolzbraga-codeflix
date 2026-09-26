package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberCommand;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.get.GetCastMemberByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CastMemberController implements CastMemberAPI {

    private static final String RESOURCE_PATH = "/cast-members/";

    private final CreateCastMemberUseCase createCastMemberUseCase;
    private final GetCastMemberByIdUseCase getCastMemberByIdUseCase;

    public CastMemberController(
            final CreateCastMemberUseCase createCastMemberUseCase,
            final GetCastMemberByIdUseCase getCastMemberByIdUseCase) {
        this.createCastMemberUseCase = createCastMemberUseCase;
        this.getCastMemberByIdUseCase = getCastMemberByIdUseCase;
    }

    @Override
    public ResponseEntity<Object> create(final CreateCastMemberRequest request) {
        final var command = CreateCastMemberCommand.with(request.name(), request.type(), request.isActive());
        return this.createCastMemberUseCase.execute(command).fold(this::unprocessableContent, this::created);
    }

    @Override
    public CastMemberResponse getById(final String id) {
        return CastMemberResponse.from(this.getCastMemberByIdUseCase.execute(id));
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateCastMemberOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id()))
                .body(CreateCastMemberResponse.from(output));
    }
}
