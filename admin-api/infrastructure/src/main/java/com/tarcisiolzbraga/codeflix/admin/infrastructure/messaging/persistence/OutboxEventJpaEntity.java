package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Uma linha por evento à espera de entrega. Sem @Audited: isto é mecanismo de entrega, não
// agregado, e o que mudou no vídeo já está auditado do lado dele.
@Entity(name = "OutboxEvent")
@Table(name = "outbox_event")
public class OutboxEventJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    // A coluna é TEXT; sem isto o Hibernate esperaria VARCHAR e a validação do schema recusaria.
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Nulo enquanto o broker não confirmou: é o que o relay procura.
    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEventJpaEntity() {
    }

    private OutboxEventJpaEntity(final String routingKey, final String payload, final Instant createdAt) {
        this.id = UUID.randomUUID();
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static OutboxEventJpaEntity pending(
            final String routingKey, final String payload, final Instant createdAt) {
        return new OutboxEventJpaEntity(routingKey, payload, createdAt);
    }

    public void markSent(final Instant sentAt) {
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return this.id;
    }

    public String getRoutingKey() {
        return this.routingKey;
    }

    public String getPayload() {
        return this.payload;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getSentAt() {
        return this.sentAt;
    }
}
