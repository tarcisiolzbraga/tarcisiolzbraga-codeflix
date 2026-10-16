package com.tarcisiolzbraga.codeflix.videos.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import org.junit.jupiter.api.Test;

class EntityTest {

    private static final String AN_ID = "a4c5f1b2";
    private static final String OTHER_ID = "9f3e7d10";
    private static final String BLANK_NAME_MESSAGE = "'name' should not be empty";

    @Test
    void givenNullId_whenInstantiate_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> new SampleEntity(null, "qualquer"));

        assertEquals("'id' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoEntitiesWithTheSameId_whenCompare_thenBeEqualEvenWithDifferentData() {
        final var one = new SampleEntity(new SampleID(AN_ID), "um nome");
        final var other = new SampleEntity(new SampleID(AN_ID), "outro nome");

        final var actualEquals = one.equals(other);

        assertTrue(actualEquals);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void givenTwoEntitiesWithDifferentIds_whenCompare_thenNotBeEqual() {
        final var one = new SampleEntity(new SampleID(AN_ID), "um nome");
        final var other = new SampleEntity(new SampleID(OTHER_ID), "um nome");

        final var actualEquals = one.equals(other);

        assertFalse(actualEquals);
    }

    @Test
    void givenEntitiesOfDifferentTypesHoldingTheSameValue_whenCompare_thenNotBeEqual() {
        final var one = new SampleEntity(new SampleID(AN_ID), "um nome");
        final var other = new OtherEntity(new OtherID(AN_ID));

        final var actualEquals = one.equals(other);

        assertFalse(actualEquals);
    }

    @Test
    void givenAnInvalidEntity_whenCallValidate_thenAppendTheErrorToTheGivenHandler() {
        final var entity = new SampleEntity(new SampleID(AN_ID), "   ");
        final var notification = Notification.create();

        entity.validate(notification);

        assertTrue(notification.hasError());
        assertEquals(BLANK_NAME_MESSAGE, notification.firstError().orElseThrow().message());
    }

    private record SampleID(String value) implements Identifier {
        @Override
        public String getValue() {
            return this.value;
        }
    }

    private record OtherID(String value) implements Identifier {
        @Override
        public String getValue() {
            return this.value;
        }
    }

    private static final class SampleEntity extends Entity<SampleID> {

        private final String name;

        private SampleEntity(final SampleID id, final String name) {
            super(id);
            this.name = name;
        }

        @Override
        public void validate(final ValidationHandler handler) {
            if (this.name == null || this.name.isBlank()) {
                handler.append(new ValidationError(BLANK_NAME_MESSAGE));
            }
        }
    }

    private static final class OtherEntity extends Entity<OtherID> {

        private OtherEntity(final OtherID id) {
            super(id);
        }

        @Override
        public void validate(final ValidationHandler handler) {
            // Nada a validar: existe só para ser um tipo diferente com o mesmo valor de id.
        }
    }
}
