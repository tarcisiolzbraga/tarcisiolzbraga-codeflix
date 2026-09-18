package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;

public interface CategoryRepository extends JpaRepository<CategoryJpaEntity, String> {

    Page<CategoryJpaEntity> findAll(Specification<CategoryJpaEntity> whereClause, Pageable page);
}
