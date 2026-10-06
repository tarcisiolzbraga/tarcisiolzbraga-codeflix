package com.tarcisiolzbraga.codeflix.videos.application.castmember.delete;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import java.util.Objects;

// Idempotente, como o delete da categoria: a remoção chega por mensagem, que pode ser reentregue.
// O id nulo, por outro lado, é recusado em vez de ignorado — é mensagem corrompida.
public class DefaultDeleteCastMemberUseCase extends DeleteCastMemberUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultDeleteCastMemberUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway =
                Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public void execute(final CastMemberID input) {
        Objects.requireNonNull(input, "'input' should not be null");
        this.castMemberGateway.deleteById(input);
    }
}
