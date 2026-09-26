package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import java.util.Optional;

public interface VideoGateway {

    Video create(Video video);

    Video update(Video video);

    void deleteById(VideoID id);

    Optional<Video> findById(VideoID id);

    // VideoSearchQuery, e não SearchQuery: a busca de vídeo filtra também por categoria, gênero e elenco.
    Pagination<Video> findAll(VideoSearchQuery query);
}
