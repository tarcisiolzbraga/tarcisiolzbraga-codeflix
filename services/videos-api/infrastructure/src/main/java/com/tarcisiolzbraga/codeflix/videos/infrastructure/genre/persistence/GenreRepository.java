package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence;

import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GenreRepository extends ElasticsearchRepository<GenreDocument, UUID> {
}
