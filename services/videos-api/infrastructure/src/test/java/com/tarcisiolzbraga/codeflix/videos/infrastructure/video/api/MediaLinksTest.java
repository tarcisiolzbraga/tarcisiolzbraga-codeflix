package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// A assinatura do endereço da mídia, sem Spring: o que importa aqui é o selo, e ele é aritmética.
class MediaLinksTest {

    private static final String VIDEO = "00000024-0000-0000-0000-000000000000";
    private static final Instant NOW = Instant.parse("2026-10-16T12:00:00Z");
    private static final Pattern SIGNED =
            Pattern.compile("^/videos/[^/]+/medias/[^?]+\\?expires=(\\d+)&signature=([A-Za-z0-9_-]+)$");

    private final MediaLinks links = new MediaLinks("um-segredo", Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void givenAVideoAndAType_whenSigns_thenTheAddressCarriesTheExpiryAndTheSignature() {
        // Given um vídeo e um tipo de mídia
        // When assina o endereço
        final var actual = this.links.signedPathFor(VIDEO, "VIDEO");

        // Then o caminho é relativo e traz validade e selo — quem sabe o host público é quem publica
        // o serviço, não ele
        assertTrue(SIGNED.matcher(actual).matches(), actual);
        assertTrue(actual.startsWith("/videos/" + VIDEO + "/medias/VIDEO?"), actual);
    }

    @Test
    void givenASignedAddress_whenChecked_thenItIsValid() {
        // Given o endereço que ele mesmo assinou
        final var matcher = SIGNED.matcher(this.links.signedPathFor(VIDEO, "VIDEO"));
        assertTrue(matcher.matches());

        // When confere / Then vale
        assertTrue(this.links.valid(VIDEO, "VIDEO", matcher.group(1), matcher.group(2)));
    }

    @Test
    void givenASignatureFromAnotherVideo_whenChecked_thenItIsRefused() {
        // Given o selo de um vídeo
        final var matcher = SIGNED.matcher(this.links.signedPathFor(VIDEO, "VIDEO"));
        assertTrue(matcher.matches());

        // When ele é apresentado para **outro** vídeo
        final var actual = this.links.valid("99999999-9999-9999-9999-999999999999", "VIDEO", matcher.group(1), matcher.group(2));

        // Then não vale: trocar o id na URL não serve para assistir outro título
        assertFalse(actual);
    }

    @Test
    void givenASignatureFromAnotherMediaType_whenChecked_thenItIsRefused() {
        // Given o selo do arquivo do filme
        final var matcher = SIGNED.matcher(this.links.signedPathFor(VIDEO, "VIDEO"));
        assertTrue(matcher.matches());

        // When ele é apresentado para o trailer / Then não vale
        assertFalse(this.links.valid(VIDEO, "TRAILER", matcher.group(1), matcher.group(2)));
    }

    @Test
    void givenAnExpiredSignature_whenChecked_thenItIsRefused() {
        // Given um selo emitido agora
        final var matcher = SIGNED.matcher(this.links.signedPathFor(VIDEO, "VIDEO"));
        assertTrue(matcher.matches());

        // When ele é conferido depois da validade, por um relógio adiantado
        final var later = new MediaLinks(
                "um-segredo", Clock.fixed(NOW.plus(MediaLinks.VALIDITY).plusSeconds(1), ZoneOffset.UTC));

        // Then não vale mais — é o limite que a técnica tem, e o que a validade curta contém
        assertFalse(later.valid(VIDEO, "VIDEO", matcher.group(1), matcher.group(2)));
    }

    @Test
    void givenAStretchedExpiry_whenChecked_thenItIsRefusedBecauseTheExpiryIsSigned() {
        // Given um selo, e alguém esticando a validade na URL
        final var matcher = SIGNED.matcher(this.links.signedPathFor(VIDEO, "VIDEO"));
        assertTrue(matcher.matches());
        final var esticada = String.valueOf(Long.parseLong(matcher.group(1)) + Duration.ofDays(30).toSeconds());

        // When confere / Then não vale: a validade entra no que foi assinado
        assertFalse(this.links.valid(VIDEO, "VIDEO", esticada, matcher.group(2)));
    }

    @Test
    void givenAnotherSecret_whenChecked_thenItIsRefused() {
        // Given um selo assinado com outro segredo, como faria quem só conhece o formato
        final var outro = new MediaLinks("outro-segredo", Clock.fixed(NOW, ZoneOffset.UTC));
        final var matcher = SIGNED.matcher(outro.signedPathFor(VIDEO, "VIDEO"));
        assertTrue(matcher.matches());

        // When confere com o segredo de verdade / Then não vale
        assertFalse(this.links.valid(VIDEO, "VIDEO", matcher.group(1), matcher.group(2)));
    }

    @MethodSource("selosMalFormados")
    @ParameterizedTest
    void givenAMalformedSignature_whenChecked_thenItIsRefusedWithoutThrowing(
            final String expires, final String signature) {
        // Given validade ou selo mal formados, que é o que chega quando alguém monta a URL à mão
        // When confere / Then recusa, e **não** estoura: exceção aqui viraria 500 em vez de 403
        assertFalse(this.links.valid(VIDEO, "VIDEO", expires, signature));
    }

    static Stream<Arguments> selosMalFormados() {
        return Stream.of(
                Arguments.of(null, "qualquer"),
                Arguments.of("123", null),
                Arguments.of("nao-e-numero", "qualquer"),
                Arguments.of("", ""),
                Arguments.of("9999999999", "selo-que-nao-confere"));
    }
}
