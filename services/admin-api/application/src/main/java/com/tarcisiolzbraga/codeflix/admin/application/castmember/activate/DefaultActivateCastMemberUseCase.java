package com.tarcisiolzbraga.codeflix.admin.application.castmember.activate;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import java.util.Objects;

public class DefaultActivateCastMemberUseCase extends ActivateCastMemberUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultActivateCastMemberUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public CastMemberOutput execute(final String input) {
        final var id = CastMemberID.from(input);
        final var castMember = this.castMemberGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(CastMember.class, id));
        castMember.activate();
        return CastMemberOutput.from(this.castMemberGateway.update(castMember));
    }
}
