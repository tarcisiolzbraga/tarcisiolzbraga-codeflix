package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreDTO;
import java.util.Optional;

public interface GenreClient {

    // Vazio quando o admin responde 404: entre o evento e esta chamada o gênero pode ter sido
    // apagado lá, com o evento de remoção a caminho.
    Optional<GenreDTO> genreOfId(String id);
}
