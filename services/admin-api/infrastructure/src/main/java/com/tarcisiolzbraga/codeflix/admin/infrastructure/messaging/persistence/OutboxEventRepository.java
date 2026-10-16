package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence;

import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    // Paginado, e não List: a fila de saída pode estar grande depois de uma indisponibilidade, e a
    // regra do projeto não admite coleção sem limite vinda do banco.
    Page<OutboxEventJpaEntity> findBySentAtIsNullOrderByCreatedAtAsc(Pageable page);

    // @Modifying aqui não fere a regra que o proíbe nos agregados: aquela existe para o Envers não
    // perder a remoção, e esta tabela não é agregado nem é auditada. Carregar as linhas só para
    // apagá-las seria desperdício numa fila que pode estar grande.
    @Modifying
    @Query("delete from OutboxEvent event where event.sentAt is not null and event.sentAt < :threshold")
    int deleteDeliveredBefore(@Param("threshold") Instant threshold);
}
