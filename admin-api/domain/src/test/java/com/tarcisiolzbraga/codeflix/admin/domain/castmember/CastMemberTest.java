package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.ThrowsValidationHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

class CastMemberTest {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Test
    void givenValidParams_whenCallNewCastMember_thenInstantiateItActiveWithEqualTimestamps() {
        final var actualMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, true);

        assertNotNull(actualMember.getId());
        assertEquals(EXPECTED_NAME, actualMember.getName());
        assertEquals(CastMemberType.ACTOR, actualMember.getType());
        assertTrue(actualMember.isActive());
        assertEquals(actualMember.getCreatedAt(), actualMember.getUpdatedAt());
    }

    @Test
    void givenInactiveFlag_whenCallNewCastMember_thenInstantiateItInactive() {
        final var actualMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.DIRECTOR, false);

        assertFalse(actualMember.isActive());
    }

    @Test
    void givenNullName_whenCallValidate_thenReceiveAnError() {
        final var actualMember = CastMember.newCastMember(null, CastMemberType.ACTOR, true);

        final var actualException =
                assertThrows(DomainException.class, () -> actualMember.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be null", actualException.getErrors().getFirst().message());
    }

    @Test
    void givenEmptyName_whenCallValidate_thenReceiveAnError() {
        final var actualMember = CastMember.newCastMember("  ", CastMemberType.ACTOR, true);

        final var actualException =
                assertThrows(DomainException.class, () -> actualMember.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be empty", actualException.getErrors().getFirst().message());
    }

    @Test
    void givenNameShorterThanThreeCharacters_whenCallValidate_thenReceiveAnError() {
        final var actualMember = CastMember.newCastMember("Vi", CastMemberType.ACTOR, true);

        final var actualException =
                assertThrows(DomainException.class, () -> actualMember.validate(new ThrowsValidationHandler()));

        assertEquals(
                "'name' must be between 3 and 255 characters",
                actualException.getErrors().getFirst().message());
    }

    @Test
    void givenNameLongerThanTwoHundredFiftyFiveCharacters_whenCallValidate_thenReceiveAnError() {
        final var actualMember = CastMember.newCastMember("a".repeat(256), CastMemberType.ACTOR, true);

        final var actualException =
                assertThrows(DomainException.class, () -> actualMember.validate(new ThrowsValidationHandler()));

        assertEquals(
                "'name' must be between 3 and 255 characters",
                actualException.getErrors().getFirst().message());
    }

    @Test
    void givenNullType_whenCallValidate_thenReceiveAnError() {
        final var actualMember = CastMember.newCastMember(EXPECTED_NAME, null, true);

        final var actualException =
                assertThrows(DomainException.class, () -> actualMember.validate(new ThrowsValidationHandler()));

        assertEquals(
                "'type' should be one of ACTOR, DIRECTOR",
                actualException.getErrors().getFirst().message());
    }

    @Test
    void givenNullNameAndNullType_whenCallValidate_thenAccumulateBothErrors() {
        final var actualMember = CastMember.newCastMember(null, null, true);
        final var notification = Notification.create();

        actualMember.validate(notification);

        assertEquals(
                List.of("'name' should not be null", "'type' should be one of ACTOR, DIRECTOR"),
                notification.getErrors().stream().map(ValidationError::message).toList());
    }

    @Test
    void givenValidParams_whenCallUpdate_thenReplaceNameAndTypeAndRefreshUpdatedAt() {
        final var actualMember = CastMember.newCastMember("Vin", CastMemberType.ACTOR, true);
        final var createdAt = actualMember.getCreatedAt();
        final var updatedAt = actualMember.getUpdatedAt();

        actualMember.update(EXPECTED_NAME, CastMemberType.DIRECTOR);

        assertEquals(EXPECTED_NAME, actualMember.getName());
        assertEquals(CastMemberType.DIRECTOR, actualMember.getType());
        assertEquals(createdAt, actualMember.getCreatedAt());
        assertTrue(updatedAt.isBefore(actualMember.getUpdatedAt()));
    }

    @Test
    void givenActiveCastMember_whenCallDeactivate_thenTurnItInactive() {
        final var actualMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, true);
        final var updatedAt = actualMember.getUpdatedAt();

        actualMember.deactivate();

        assertFalse(actualMember.isActive());
        assertTrue(updatedAt.isBefore(actualMember.getUpdatedAt()));
    }

    @Test
    void givenPersistedValues_whenCallWith_thenRebuildTheSameCastMember() {
        final var expectedMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, true);

        final var actualMember = CastMember.with(
                expectedMember.getId(),
                expectedMember.getName(),
                expectedMember.getType(),
                expectedMember.isActive(),
                expectedMember.getCreatedAt(),
                expectedMember.getUpdatedAt());

        assertEquals(expectedMember, actualMember);
        assertEquals(expectedMember.getName(), actualMember.getName());
        assertEquals(expectedMember.getType(), actualMember.getType());
        assertEquals(expectedMember.getCreatedAt(), actualMember.getCreatedAt());
    }
}
