package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoPreview;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<VideoJpaEntity, String> {

    // Um conjunto nulo significa "não filtra por isso"; conjunto vazio nunca chega aqui, porque o
    // gateway troca por nulo antes de consultar.
    //
    // A consulta monta a prévia, não a entidade: sem isto a página traria os vínculos e as cinco
    // mídias de cada vídeo para escrever seis campos. A contagem vem escrita à mão porque distinct
    // junto de expressão de construtor derrota a derivação automática.
    @Query(value = """
            select distinct new com.tarcisiolzbraga.codeflix.admin.domain.video.VideoPreview(
                v.id, v.title, v.yearLaunched, v.published, v.active, v.createdAt)
            from Video v
                left join v.categories categoryLink
                left join v.genres genreLink
                left join v.castMembers memberLink
            where (:terms is null or upper(v.title) like :terms)
              and (:categories is null or categoryLink.id.categoryId in :categories)
              and (:genres is null or genreLink.id.genreId in :genres)
              and (:castMembers is null or memberLink.id.castMemberId in :castMembers)
            """,
            countQuery = """
            select count(distinct v.id)
            from Video v
                left join v.categories categoryLink
                left join v.genres genreLink
                left join v.castMembers memberLink
            where (:terms is null or upper(v.title) like :terms)
              and (:categories is null or categoryLink.id.categoryId in :categories)
              and (:genres is null or genreLink.id.genreId in :genres)
              and (:castMembers is null or memberLink.id.castMemberId in :castMembers)
            """)
    Page<VideoPreview> findAll(
            @Param("terms") String terms,
            @Param("categories") Set<String> categories,
            @Param("genres") Set<String> genres,
            @Param("castMembers") Set<String> castMembers,
            Pageable page);

    @Query("select count(link) > 0 from VideoCategory link where link.id.categoryId = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") String categoryId);

    @Query("select count(link) > 0 from VideoGenre link where link.id.genreId = :genreId")
    boolean existsByGenreId(@Param("genreId") String genreId);

    @Query("select count(link) > 0 from VideoCastMember link where link.id.castMemberId = :castMemberId")
    boolean existsByCastMemberId(@Param("castMemberId") String castMemberId);
}
