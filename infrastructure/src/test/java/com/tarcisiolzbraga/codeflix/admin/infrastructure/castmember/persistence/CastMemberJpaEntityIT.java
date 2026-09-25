package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class CastMemberJpaEntityIT {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Autowired
    private CastMemberRepository castMemberRepository;

    @Test
    void givenCastMember_whenSaveAndReload_thenKeepAllFields() {
        final var castMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, true);

        final var actualCastMember = saveAndReload(castMember);

        assertEquals(castMember.getId(), actualCastMember.getId());
        assertEquals(EXPECTED_NAME, actualCastMember.getName());
        assertEquals(CastMemberType.ACTOR, actualCastMember.getType());
        assertEquals(castMember.getCreatedAt(), actualCastMember.getCreatedAt());
        assertEquals(castMember.getUpdatedAt(), actualCastMember.getUpdatedAt());
    }

    @Test
    void givenInactiveCastMember_whenSaveAndReload_thenKeepItInactive() {
        final var castMember = CastMember.newCastMember("Steven Spielberg", CastMemberType.DIRECTOR, false);

        final var actualCastMember = saveAndReload(castMember);

        assertFalse(actualCastMember.isActive());
        assertEquals(CastMemberType.DIRECTOR, actualCastMember.getType());
    }

    @Test
    void givenCastMemberWithoutName_whenSave_thenFailByTheNotNullColumn() {
        final var castMember = CastMember.newCastMember(null, CastMemberType.ACTOR, true);
        final var entity = CastMemberJpaEntity.from(castMember);

        assertThrows(DataIntegrityViolationException.class, () -> this.castMemberRepository.saveAndFlush(entity));
    }

    private CastMember saveAndReload(final CastMember castMember) {
        this.castMemberRepository.saveAndFlush(CastMemberJpaEntity.from(castMember));
        return this.castMemberRepository
                .findById(castMember.getId().getValue())
                .orElseThrow()
                .toAggregate();
    }
}
