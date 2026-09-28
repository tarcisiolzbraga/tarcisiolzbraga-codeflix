package com.tarcisiolzbraga.codeflix.admin.application.castmember.delete;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import java.util.Objects;

public class DefaultDeleteCastMemberUseCase extends DeleteCastMemberUseCase {

    private final CastMemberGateway castMemberGateway;
    private final VideoGateway videoGateway;

    public DefaultDeleteCastMemberUseCase(final CastMemberGateway castMemberGateway, final VideoGateway videoGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    // Agregado em uso não é removido, só desativado; a foreign key sem cascata segura o que escapar daqui.
    @Override
    public void execute(final String input) {
        final var id = CastMemberID.from(input);
        if (this.videoGateway.existsByCastMember(id)) {
            throw ConflictException.linked(CastMember.class, id, Video.class);
        }
        this.castMemberGateway.deleteById(id);
    }
}
