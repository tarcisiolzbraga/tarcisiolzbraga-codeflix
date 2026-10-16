package com.tarcisiolzbraga.codeflix.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@MappedSuperclass
public abstract class BaseJpaEntity {

    // UUID, e não String: o id sempre foi um, e o tipo passa a dizer. O @JdbcTypeCode(CHAR)
    // mantém a coluna como está — CHAR(36) com a forma canônica —, então não há migration nem
    // mudança no que já está gravado; muda só o que o Java carrega.
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BaseJpaEntity() {
    }

    protected BaseJpaEntity(
            final UUID id, final boolean active, final Instant createdAt, final Instant updatedAt) {
        this.id = id;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return this.id;
    }

    public boolean isActive() {
        return this.active;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
