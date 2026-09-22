package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenreRepository extends JpaRepository<GenreJpaEntity, String> {

    Page<GenreJpaEntity> findAll(Specification<GenreJpaEntity> whereClause, Pageable page);

    @Query("select count(link) > 0 from GenreCategory link where link.id.categoryId = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") String categoryId);
}
