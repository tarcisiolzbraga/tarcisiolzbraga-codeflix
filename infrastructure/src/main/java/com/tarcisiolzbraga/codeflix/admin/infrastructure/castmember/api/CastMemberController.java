package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.api;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberCommand;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.get.GetCastMemberByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.ListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberCommand;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberSearchRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.UpdateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.UpdateCastMemberResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CastMemberController implements CastMemberAPI {

    private static final String RESOURCE_PATH = "/cast-members/";

    private final CreateCastMemberUseCase createCastMemberUseCase;
    private final GetCastMemberByIdUseCase getCastMemberByIdUseCase;
    private final ListCastMembersUseCase listCastMembersUseCase;
    private final UpdateCastMemberUseCase updateCastMemberUseCase;

    public CastMemberController(
            final CreateCastMemberUseCase createCastMemberUseCase,
            final GetCastMemberByIdUseCase getCastMemberByIdUseCase,
            final ListCastMembersUseCase listCastMembersUseCase,
            final UpdateCastMemberUseCase updateCastMemberUseCase) {
        this.createCastMemberUseCase = createCastMemberUseCase;
        this.getCastMemberByIdUseCase = getCastMemberByIdUseCase;
        this.listCastMembersUseCase = listCastMembersUseCase;
        this.updateCastMemberUseCase = updateCastMemberUseCase;
    }

    @Override
    public ResponseEntity<Object> create(final CreateCastMemberRequest request) {
        final var command = CreateCastMemberCommand.with(request.name(), request.type(), request.isActive());
        return this.createCastMemberUseCase.execute(command).fold(this::unprocessableContent, this::created);
    }

    @Override
    public Pagination<CastMemberListResponse> list(final CastMemberSearchRequest request) {
        return this.listCastMembersUseCase.execute(request.toSearchQuery()).map(CastMemberListResponse::from);
    }

    @Override
    public CastMemberResponse getById(final String id) {
        return CastMemberResponse.from(this.getCastMemberByIdUseCase.execute(id));
    }

    @Override
    public ResponseEntity<Object> update(final String id, final UpdateCastMemberRequest request) {
        final var command = UpdateCastMemberCommand.with(id, request.name(), request.type());
        return this.updateCastMemberUseCase.execute(command).fold(this::unprocessableContent, this::updated);
    }

    private ResponseEntity<Object> unprocessableContent(final Notification notification) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(notification));
    }

    private ResponseEntity<Object> created(final CreateCastMemberOutput output) {
        return ResponseEntity.created(URI.create(RESOURCE_PATH + output.id()))
                .body(CreateCastMemberResponse.from(output));
    }

    private ResponseEntity<Object> updated(final UpdateCastMemberOutput output) {
        return ResponseEntity.ok(UpdateCastMemberResponse.from(output));
    }
}
