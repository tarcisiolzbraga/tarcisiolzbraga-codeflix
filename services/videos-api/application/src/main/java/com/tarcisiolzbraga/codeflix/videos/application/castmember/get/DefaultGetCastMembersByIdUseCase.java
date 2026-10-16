package com.tarcisiolzbraga.codeflix.videos.application.castmember.get;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultGetCastMembersByIdUseCase extends GetCastMembersByIdUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultGetCastMembersByIdUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway =
                Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public List<CastMemberOutput> execute(final Set<CastMemberID> input) {
        Objects.requireNonNull(input, "'input' should not be null");
        if (input.isEmpty()) {
            return List.of();
        }

        return this.castMemberGateway.findAllById(input).stream()
                .map(CastMemberOutput::from)
                .toList();
    }
}
