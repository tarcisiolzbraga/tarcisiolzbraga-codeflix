package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class CreateVideoRequestTest {

    @Autowired
    private JacksonTester<CreateVideoRequest> json;

    @Test
    void givenJsonWithEveryField_whenDeserialize_thenReadThemAll() throws IOException {
        final var content =
                """
                {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"12",
                 "categories":["c1"],"genres":["g1"],"castMembers":["m1"]}""";

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Duna", actualRequest.title());
        assertEquals(2021, actualRequest.launchedAt());
        assertEquals(155.0, actualRequest.duration());
        assertEquals("12", actualRequest.rating());
        assertEquals(Set.of("c1"), actualRequest.categories());
        assertEquals(Set.of("g1"), actualRequest.genres());
        assertEquals(Set.of("m1"), actualRequest.castMembers());
    }

    @Test
    void givenJsonWithoutReferences_whenCallToReferences_thenReturnEmptySets() throws IOException {
        final var content = """
                {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"12"}""";

        final var actualRequest = this.json.parseObject(content);

        assertNull(actualRequest.categories());
        assertTrue(actualRequest.toReferences().categories().isEmpty());
        assertTrue(actualRequest.toReferences().genres().isEmpty());
    }

    @Test
    void givenJsonWithUnknownRating_whenDeserialize_thenKeepTheRawText() throws IOException {
        final var content = """
                {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"99"}""";

        final var actualRequest = this.json.parseObject(content);

        assertEquals("99", actualRequest.toFields().rating());
    }

    @Test
    void givenJsonWithActive_whenDeserialize_thenIgnoreIt() throws IOException {
        final var content = """
                {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"12",
                 "active":false,"published":true}""";

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Duna", actualRequest.title());
    }
}
