package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CastMemberRepository extends ElasticsearchRepository<CastMemberDocument, String> {
}
