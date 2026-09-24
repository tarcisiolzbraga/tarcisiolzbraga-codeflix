package com.tarcisiolzbraga.codeflix.admin.application.castmember.delete;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import java.util.Objects;

public class DefaultDeleteCastMemberUseCase extends DeleteCastMemberUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultDeleteCastMemberUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public void execute(final String input) {
        this.castMemberGateway.deleteById(CastMemberID.from(input));
    }
}
