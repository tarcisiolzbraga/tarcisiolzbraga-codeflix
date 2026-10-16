package com.tarcisiolzbraga.codeflix.admin.infrastructure.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    // O `sub` do token de quem gravou, ou nulo quando não houve requisição autenticada. Quem o
    // preenche é o AuditRevisionListener, ao lado.
    @Column(name = "user_id")
    private String userId;

    public int getId() {
        return this.id;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getUserId() {
        return this.userId;
    }

    // Visível só no pacote, e não público: o único que deve chamar é o listener ao lado. O
    // Hibernate não precisa dele — lê e escreve o campo direto, porque o @Id está no campo.
    void setUserId(final String userId) {
        this.userId = userId;
    }
}
