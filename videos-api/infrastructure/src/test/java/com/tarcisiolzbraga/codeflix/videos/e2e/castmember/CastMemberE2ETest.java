package com.tarcisiolzbraga.codeflix.videos.e2e.castmember;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.CastMemberClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.GqlCastMember;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// A jornada de verdade deste serviço: a mudança chega do admin-codeflix por change data capture e o
// cliente a lê por HTTP, no GraphQL. Cobre mais do que a jornada da categoria, que monta o estado
// pela mutation de exemplo — aqui o estado entra pelo caminho real.
//
// O único simulado é o CastMemberClient, que fala com a API do admin: ela não faz parte deste
// serviço e não sobe aqui. Tomcat, Kafka e Elasticsearch são de verdade.
@E2ETest
class CastMemberE2ETest implements CastMemberE2EDsl {

    private static final Duration VISIBLE = Duration.ofSeconds(40);

    @Value("${local.server.port}")
    private int port;

    @Value("${kafka.consumers.cast-member.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private CastMemberClient castMemberClient;

    @Autowired
    private KeycloakTestToken token;

    private RestClient client;

    @BeforeEach
    void bindToTheRunningServer() {
        this.client = clientWith(this.token.subscriber());
    }

    // O token vai no cabeçalho padrão do cliente: é como um consumidor de verdade fala com a API,
    // e é o que faz esta jornada atravessar também a autorização.
    private RestClient clientWith(final String bearer) {
        return RestClient.builder()
                .baseUrl("http://localhost:%d/api".formatted(this.port))
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer)
                .build();
    }

    @Override
    public RestClient client() {
        return this.client;
    }

    @Test
    void givenAChangeArrivingFromTheAdmin_whenAClientListsThem_thenSeeItOverHttp() {
        givenAMemberPublishedByTheAdmin("00000003-6000-0000-0000-000000000000", "Denis Villeneuve", "DIRECTOR");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCastMembers();

            assertEquals(1L, actualPage.meta().total());
            final var actualMember = actualPage.items().getFirst();
            assertEquals("00000003-6000-0000-0000-000000000000", actualMember.id());
            assertEquals("Denis Villeneuve", actualMember.name());
            assertEquals(CastMemberType.DIRECTOR, actualMember.type());
        });
    }

    @Test
    void givenSeveralMembersFromTheAdmin_whenAClientListsThem_thenSeeThemSortedWithTheirPageNumbers() {
        givenAMemberPublishedByTheAdmin("00000003-6000-0000-0000-000000000000", "Denis Villeneuve", "DIRECTOR");
        givenAMemberPublishedByTheAdmin("00000003-7000-0000-0000-000000000000", "Rebecca Ferguson", "ACTOR");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCastMembers();

            assertEquals(0, actualPage.meta().currentPage());
            assertEquals(10, actualPage.meta().perPage());
            assertEquals(2L, actualPage.meta().total());
            assertEquals(List.of("Denis Villeneuve", "Rebecca Ferguson"), namesOf(actualPage.items()));
        });
    }

    @Test
    void givenMembersFromTheAdmin_whenSearchByName_thenSeeOnlyWhatMatches() {
        givenAMemberPublishedByTheAdmin("00000003-6000-0000-0000-000000000000", "Denis Villeneuve", "DIRECTOR");
        givenAMemberPublishedByTheAdmin("00000003-7000-0000-0000-000000000000", "Rebecca Ferguson", "ACTOR");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCastMembers("Rebecca");

            assertEquals(1L, actualPage.meta().total());
            assertEquals(List.of("Rebecca Ferguson"), namesOf(actualPage.items()));
        });
    }

    @Test
    void givenNothingPublished_whenAClientListsThem_thenSeeAnEmptyPage() {
        final var actualPage = listCastMembers();

        assertEquals(0L, actualPage.meta().total());
        assertTrue(actualPage.items().isEmpty());
    }

    // Publica no tópico a mensagem que o Debezium publicaria, com o id e o conteúdo deste caso. O
    // envelope é o capturado do admin; só o id muda, para um teste não ver mensagem de outro.
    private void givenAMemberPublishedByTheAdmin(final String id, final String name, final String type) {
        when(this.castMemberClient.castMemberOfId(id))
                .thenReturn(Optional.of(new CastMemberDTO(
                        id,
                        name,
                        type,
                        true,
                        Instant.parse("2026-09-30T12:00:00Z"),
                        Instant.parse("2026-10-01T08:30:00Z"))));
        try {
            final var json = new String(
                    new ClassPathResource("cdc/cast-member-c.json").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            final var root = (ObjectNode) this.mapper.readTree(json);
            ((ObjectNode) root.get("payload").get("after")).put("id", id);
            this.testKafkaTemplate.send(this.topic, id, this.mapper.writeValueAsString(root)).get();
        } catch (final Exception e) {
            throw new IllegalStateException("não foi possível publicar a mudança de " + id, e);
        }
    }

    private static List<String> namesOf(final List<GqlCastMember> items) {
        return items.stream().map(GqlCastMember::name).sorted().toList();
    }
}
