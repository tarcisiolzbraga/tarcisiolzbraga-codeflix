package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence;

import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CastMemberRepository extends ElasticsearchRepository<CastMemberDocument, UUID> {
}
