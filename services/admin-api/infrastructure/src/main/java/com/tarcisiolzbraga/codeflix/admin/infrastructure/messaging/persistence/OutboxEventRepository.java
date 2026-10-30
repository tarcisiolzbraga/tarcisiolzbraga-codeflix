package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    // Paginado, e não List: a fila de saída pode estar grande depois de uma indisponibilidade, e a
    // regra do projeto não admite coleção sem limite vinda do banco.
    //
    // Continua aqui porque o teste e o diagnóstico leem por ela; quem **entrega** usa o
    // lockNextPending abaixo.
    Page<OutboxEventJpaEntity> findBySentAtIsNullOrderByCreatedAtAsc(Pageable page);

    // A linha mais antiga ainda não entregue, **travada para escrita e saltando o que já está
    // travado**. É isto que permite mais de uma instância do relay: cada uma pega uma linha
    // diferente, em vez de as duas lerem a mesma e entregarem em dobro.
    //
    // O `-2` é o `LockOptions.SKIP_LOCKED` do Hibernate. Sem ele o comportamento seria esperar o
    // lock (ou estourar o tempo), e aí a segunda instância ficaria parada atrás da primeira em vez
    // de trabalhar — o que troca entrega dupla por serialização, e nenhum dos dois é o que se quer.
    //
    // Uma linha por chamada, e não um lote: o lock vale até o fim da transação, e a transação aqui é
    // de uma entrega só, para falha no meio da passagem não desfazer o que já saiu. Ver o OutboxRelay.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select event from OutboxEvent event where event.sentAt is null order by event.createdAt asc limit 1")
    Optional<OutboxEventJpaEntity> lockNextPending();

    // @Modifying aqui não fere a regra que o proíbe nos agregados: aquela existe para o Envers não
    // perder a remoção, e esta tabela não é agregado nem é auditada. Carregar as linhas só para
    // apagá-las seria desperdício numa fila que pode estar grande.
    @Modifying
    @Query("delete from OutboxEvent event where event.sentAt is not null and event.sentAt < :threshold")
    int deleteDeliveredBefore(@Param("threshold") Instant threshold);
}
