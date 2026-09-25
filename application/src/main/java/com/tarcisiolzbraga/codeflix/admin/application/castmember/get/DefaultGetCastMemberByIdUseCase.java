package com.tarcisiolzbraga.codeflix.admin.application.castmember.get;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import java.util.Objects;

public class DefaultGetCastMemberByIdUseCase extends GetCastMemberByIdUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultGetCastMemberByIdUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public CastMemberOutput execute(final String input) {
        final var id = CastMemberID.from(input);
        return this.castMemberGateway
                .findById(id)
                .map(CastMemberOutput::from)
                .orElseThrow(() -> NotFoundException.with(CastMember.class, id));
    }
}
