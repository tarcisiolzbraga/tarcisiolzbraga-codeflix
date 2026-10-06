package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CastMemberRepository extends JpaRepository<CastMemberJpaEntity, String> {

    Page<CastMemberJpaEntity> findAll(Specification<CastMemberJpaEntity> whereClause, Pageable page);

    @Query("select m.id from CastMember m where m.id in :ids")
    Set<String> findExistingIds(@Param("ids") Set<String> ids);
}
