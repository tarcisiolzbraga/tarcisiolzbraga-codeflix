package com.tarcisiolzbraga.codeflix.admin.e2e;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.CreateCastMemberResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models.UpdateCastMemberRequest;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP, sem atalho pelo gateway ou pelo banco.
public interface CastMemberE2EDsl {

    String CAST_MEMBERS_PATH = "/cast-members";

    RestClient client();

    default String givenACastMember(final String name, final String type) {
        return createACastMember(new CreateCastMemberRequest(name, type, null)).id();
    }

    default CreateCastMemberResponse createACastMember(final CreateCastMemberRequest request) {
        return client().post()
                .uri(CAST_MEMBERS_PATH)
                .body(request)
                .retrieve()
                .body(CreateCastMemberResponse.class);
    }

    default CastMemberResponse retrieveACastMember(final String id) {
        return client().get()
                .uri(CAST_MEMBERS_PATH + "/{id}", id)
                .retrieve()
                .body(CastMemberResponse.class);
    }

    default Pagination<CastMemberListResponse> listCastMembers(final int page, final int perPage) {
        return listCastMembers(page, perPage, null);
    }

    default Pagination<CastMemberListResponse> listCastMembers(
            final int page, final int perPage, final String search) {
        return client().get()
                .uri(builder -> builder.path(CAST_MEMBERS_PATH)
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParamIfPresent("search", Optional.ofNullable(search))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    default void updateACastMember(final String id, final String name, final String type) {
        client().put()
                .uri(CAST_MEMBERS_PATH + "/{id}", id)
                .body(new UpdateCastMemberRequest(name, type))
                .retrieve()
                .toBodilessEntity();
    }

    default CastMemberResponse activateACastMember(final String id) {
        return changeActivation(id, "activate");
    }

    default CastMemberResponse deactivateACastMember(final String id) {
        return changeActivation(id, "deactivate");
    }

    default void deleteACastMember(final String id) {
        client().delete().uri(CAST_MEMBERS_PATH + "/{id}", id).retrieve().toBodilessEntity();
    }

    private CastMemberResponse changeActivation(final String id, final String action) {
        return client().put()
                .uri(CAST_MEMBERS_PATH + "/{id}/{action}", id, action)
                .retrieve()
                .body(CastMemberResponse.class);
    }
}
