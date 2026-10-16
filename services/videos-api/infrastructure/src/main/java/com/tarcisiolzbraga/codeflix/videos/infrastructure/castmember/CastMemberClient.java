package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberDTO;
import java.util.Optional;

public interface CastMemberClient {

    // Vazio quando o admin responde 404, que acontece de verdade e não é erro: entre o evento de
    // criação e esta chamada o membro pode ter sido apagado lá, com o evento de remoção a caminho.
    Optional<CastMemberDTO> castMemberOfId(String id);
}
