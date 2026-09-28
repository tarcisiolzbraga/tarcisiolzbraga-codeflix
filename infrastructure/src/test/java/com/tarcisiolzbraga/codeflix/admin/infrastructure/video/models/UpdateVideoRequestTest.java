package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class UpdateVideoRequestTest {

    @Autowired
    private JacksonTester<UpdateVideoRequest> json;

    @Test
    void givenJsonWithEveryField_whenDeserialize_thenReadThemAll() throws IOException {
        final var content =
                """
                {"title":"Duna: Parte 2","description":"Arrakis","launchedAt":2024,"duration":166.0,
                 "rating":"14","categories":["c1"],"genres":["g1"],"castMembers":["m1"]}""";

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Duna: Parte 2", actualRequest.title());
        assertEquals(2024, actualRequest.toFields().launchedAt());
        assertEquals("14", actualRequest.toFields().rating());
        assertEquals(Set.of("c1"), actualRequest.toReferences().categories());
    }

    @Test
    void givenJsonWithoutReferences_whenCallToReferences_thenReturnEmptySets() throws IOException {
        final var content = """
                {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"12"}""";

        final var actualRequest = this.json.parseObject(content);

        assertTrue(actualRequest.toReferences().castMembers().isEmpty());
    }

    @Test
    void givenJsonWithPublished_whenDeserialize_thenIgnoreIt() throws IOException {
        final var content =
                """
                {"title":"Duna","description":"Arrakis","launchedAt":2021,"duration":155.0,"rating":"12",
                 "published":true,"opened":true,"active":false}""";

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Duna", actualRequest.title());
    }
}
