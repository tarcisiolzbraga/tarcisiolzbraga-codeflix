package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, String> {

    // Paginado, e não List: a fila de saída pode estar grande depois de uma indisponibilidade, e a
    // regra do projeto não admite coleção sem limite vinda do banco.
    Page<OutboxEventJpaEntity> findBySentAtIsNullOrderByCreatedAtAsc(Pageable page);
}
