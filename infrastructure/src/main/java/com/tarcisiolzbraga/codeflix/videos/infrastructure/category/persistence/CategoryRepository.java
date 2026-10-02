package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.persistence;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CategoryRepository extends ElasticsearchRepository<CategoryDocument, String> {
}
