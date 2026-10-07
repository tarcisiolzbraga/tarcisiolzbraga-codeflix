package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.persistence;

import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoRepository extends ElasticsearchRepository<VideoDocument, UUID> {
}
