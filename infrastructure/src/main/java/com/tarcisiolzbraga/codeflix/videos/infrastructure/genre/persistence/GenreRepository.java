package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GenreRepository extends ElasticsearchRepository<GenreDocument, String> {
}
