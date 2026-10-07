package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    Page<CategoryJpaEntity> findAll(Specification<CategoryJpaEntity> whereClause, Pageable page);

    @Query("select c.id from Category c where c.id in :ids")
    Set<UUID> findExistingIds(@Param("ids") Set<UUID> ids);
}
