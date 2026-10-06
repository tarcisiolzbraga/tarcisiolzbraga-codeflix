package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence.CastMemberJpaEntity;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CastMemberAuditIT {

    private static final String VIN_DIESEL = "Vin Diesel";

    @Autowired
    private CastMemberMySQLGateway castMemberGateway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void givenCreatedCastMember_whenReadFirstRevision_thenHaveTheNameAndType() {
        final var castMember = givenPersistedCastMember();

        final var revisions = revisionsOf(castMember.getId().getValue());

        assertEquals(1, revisions.size());
        final var audited = auditedAt(castMember.getId().getValue(), revisions.getFirst());
        assertEquals(VIN_DIESEL, audited.getName());
        assertEquals(CastMemberType.ACTOR, audited.getType());
    }

    @Test
    void givenUpdatedCastMember_whenReadRevisions_thenKeepTheOldTypeInThePreviousOne() {
        final var castMember = givenPersistedCastMember();
        this.castMemberGateway.update(castMember.update(VIN_DIESEL, CastMemberType.DIRECTOR));

        final var revisions = revisionsOf(castMember.getId().getValue());

        assertEquals(2, revisions.size());
        assertEquals(
                CastMemberType.ACTOR,
                auditedAt(castMember.getId().getValue(), revisions.getFirst()).getType());
        assertEquals(
                CastMemberType.DIRECTOR,
                auditedAt(castMember.getId().getValue(), revisions.get(1)).getType());
    }

    @Test
    void givenDeactivatedCastMember_whenReadRevisions_thenRecordTheChange() {
        final var castMember = givenPersistedCastMember();
        castMember.deactivate();
        this.castMemberGateway.update(castMember);

        final var revisions = revisionsOf(castMember.getId().getValue());

        assertFalse(auditedAt(castMember.getId().getValue(), revisions.get(1)).isActive());
    }

    @Test
    void givenDeletedCastMember_whenReadRevisions_thenRecordTheDeletion() {
        final var castMember = givenPersistedCastMember();
        this.castMemberGateway.deleteById(castMember.getId());

        final var revisions = revisionsOf(castMember.getId().getValue());

        assertEquals(2, revisions.size());
        assertNull(auditedAt(castMember.getId().getValue(), revisions.get(1)));
    }

    private CastMember givenPersistedCastMember() {
        return this.castMemberGateway.create(CastMember.newCastMember(VIN_DIESEL, CastMemberType.ACTOR, true));
    }

    private List<Number> revisionsOf(final String id) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).getRevisions(CastMemberJpaEntity.class, id);
        }
    }

    private CastMemberJpaEntity auditedAt(final String id, final Number revision) {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            return AuditReaderFactory.get(entityManager).find(CastMemberJpaEntity.class, id, revision);
        }
    }
}
