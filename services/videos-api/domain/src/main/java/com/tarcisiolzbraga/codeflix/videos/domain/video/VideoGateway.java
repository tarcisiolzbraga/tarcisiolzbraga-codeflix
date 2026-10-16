package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import java.util.Optional;

// Sem findAllById: nada referencia vídeo. O vídeo é a ponta da cadeia — ele aponta para categoria,
// gênero e membro de elenco, e nenhum deles aponta de volta.
public interface VideoGateway {

    Video save(Video video);

    void deleteById(VideoID id);

    Optional<Video> findById(VideoID id);

    Pagination<Video> findAll(VideoSearchQuery query);
}
