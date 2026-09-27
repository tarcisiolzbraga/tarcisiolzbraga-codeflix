package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<VideoJpaEntity, String> {

    // Um conjunto nulo significa "não filtra por isso"; conjunto vazio nunca chega aqui, porque o
    // gateway troca por nulo antes de consultar.
    @Query("""
            select distinct v from Video v
                left join v.categories categoryLink
                left join v.genres genreLink
                left join v.castMembers memberLink
            where (:terms is null or upper(v.title) like :terms)
              and (:categories is null or categoryLink.id.categoryId in :categories)
              and (:genres is null or genreLink.id.genreId in :genres)
              and (:castMembers is null or memberLink.id.castMemberId in :castMembers)
            """)
    Page<VideoJpaEntity> findAll(
            @Param("terms") String terms,
            @Param("categories") Set<String> categories,
            @Param("genres") Set<String> genres,
            @Param("castMembers") Set<String> castMembers,
            Pageable page);
}
