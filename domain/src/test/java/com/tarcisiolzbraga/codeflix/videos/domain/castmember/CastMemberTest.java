package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.ThrowsValidationHandler;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CastMemberTest {

    private static final CastMemberID EXPECTED_ID = CastMemberID.from("7c1e3d4a-9b2f-4c80-8d61-a2b3c4d5e6f7");
    private static final String EXPECTED_NAME = "Denis Villeneuve";
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final String NAME_LENGTH_MESSAGE = "'name' must be between 3 and 255 characters";
    private static final String TYPE_MESSAGE = "'type' should be one of ACTOR, DIRECTOR";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateTheReplicaAsReceived() {
        final var actualMember = aMember(EXPECTED_NAME, CastMemberType.DIRECTOR);

        assertEquals(EXPECTED_ID, actualMember.getId());
        assertEquals(EXPECTED_NAME, actualMember.getName());
        assertEquals(CastMemberType.DIRECTOR, actualMember.getType());
        assertTrue(actualMember.isActive());
        assertEquals(EXPECTED_CREATED_AT, actualMember.getCreatedAt());
        assertEquals(EXPECTED_UPDATED_AT, actualMember.getUpdatedAt());
    }

    @Test
    void givenAnInactiveMember_whenCallWith_thenKeepItInactive() {
        final var actualMember = CastMember.with(
                EXPECTED_ID, EXPECTED_NAME, CastMemberType.ACTOR, false, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);

        assertFalse(actualMember.isActive());
    }

    @Test
    void givenAMember_whenCallWithCopy_thenReturnAnotherInstanceHoldingTheSameData() {
        final var member = aMember(EXPECTED_NAME, CastMemberType.DIRECTOR);

        final var actualCopy = CastMember.with(member);

        assertNotSame(member, actualCopy);
        assertEquals(member.getId(), actualCopy.getId());
        assertEquals(member.getName(), actualCopy.getName());
        assertEquals(member.getType(), actualCopy.getType());
        assertEquals(member.isActive(), actualCopy.isActive());
        assertEquals(member.getCreatedAt(), actualCopy.getCreatedAt());
        assertEquals(member.getUpdatedAt(), actualCopy.getUpdatedAt());
    }

    @Test
    void givenValidParams_whenCallValidate_thenAccumulateNoError() {
        final var member = aMember(EXPECTED_NAME, CastMemberType.ACTOR);
        final var notification = Notification.create();

        member.validate(notification);

        assertFalse(notification.hasError());
    }

    @Test
    void givenNullName_whenCallValidate_thenThrowDomainException() {
        final var member = aMember(null, CastMemberType.ACTOR);

        final var actualException =
                assertThrows(DomainException.class, () -> member.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenBlankName_whenCallValidate_thenThrowDomainException() {
        final var member = aMember("   ", CastMemberType.ACTOR);

        final var actualException =
                assertThrows(DomainException.class, () -> member.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be empty", actualException.getMessage());
    }

    @Test
    void givenNameShorterThanTheMinimum_whenCallValidate_thenThrowDomainException() {
        final var member = aMember("Al ", CastMemberType.ACTOR);

        final var actualException =
                assertThrows(DomainException.class, () -> member.validate(new ThrowsValidationHandler()));

        assertEquals(NAME_LENGTH_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenNameLongerThanTheMaximum_whenCallValidate_thenThrowDomainException() {
        final var member = aMember("a".repeat(256), CastMemberType.ACTOR);

        final var actualException =
                assertThrows(DomainException.class, () -> member.validate(new ThrowsValidationHandler()));

        assertEquals(NAME_LENGTH_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenNullType_whenCallValidate_thenThrowDomainExceptionNamingTheTypesThatExist() {
        final var member = aMember(EXPECTED_NAME, null);

        final var actualException =
                assertThrows(DomainException.class, () -> member.validate(new ThrowsValidationHandler()));

        assertEquals(TYPE_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenBlankId_whenCallValidate_thenThrowDomainExceptionBecauseTheMessageCameCorrupted() {
        final var member = CastMember.with(
                CastMemberID.from("  "),
                EXPECTED_NAME,
                CastMemberType.ACTOR,
                true,
                EXPECTED_CREATED_AT,
                EXPECTED_UPDATED_AT);

        final var actualException =
                assertThrows(DomainException.class, () -> member.validate(new ThrowsValidationHandler()));

        assertEquals("'id' should not be empty", actualException.getMessage());
    }

    @Test
    void givenEverythingWrong_whenCallValidateWithNotification_thenAccumulateEveryError() {
        final var member = CastMember.with(
                CastMemberID.from(""), "  ", null, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
        final var notification = Notification.create();

        member.validate(notification);

        assertEquals(3, notification.getErrors().size());
        assertEquals("'id' should not be empty", notification.firstError().orElseThrow().message());
        assertEquals("'name' should not be empty", notification.getErrors().get(1).message());
        assertEquals(TYPE_MESSAGE, notification.getErrors().get(2).message());
    }

    private static CastMember aMember(final String name, final CastMemberType type) {
        return CastMember.with(EXPECTED_ID, name, type, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
    }
}
