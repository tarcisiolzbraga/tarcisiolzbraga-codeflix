package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Assina o endereço da mídia, para o `<video>` do front poder apontar para ele.
//
// **Existe porque `<video src>` não carrega cabeçalho de Authorization.** O browser busca o `src` por
// conta própria, sem token. Sem uma URL que se autorize sozinha, a única saída é baixar o arquivo
// inteiro por `fetch` antes de tocar — o que o portal fazia, e custava a reprodução progressiva e a
// busca na linha de tempo.
//
// A assinatura **não é um token**: ela não concede nada além de um vídeo, de um tipo de mídia, por
// alguns minutos. Quem a emite é a rota de link, que confere papel antes; aqui só se produz e se
// confere o selo.
@Component
public class MediaLinks {

    // Minutos, e não horas: URL assinada copiada vale até expirar, e esse é o limite inerente da
    // técnica. Cinco minutos é tempo de clicar e começar a assistir, não de compartilhar.
    static final Duration VALIDITY = Duration.ofMinutes(5);

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    private final Clock clock;

    public MediaLinks(@Value("${media.signing-secret}") final String secret, final Clock clock) {
        // Derruba a subida como o resto da configuração do monorepo: segredo vazio ou com placeholder
        // não resolvido faria o serviço assinar com nada e aceitar qualquer coisa.
        if (secret == null || secret.isBlank() || secret.contains("${")) {
            throw new IllegalStateException("'media.signing-secret' is required and was not resolved");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
    }

    // O endereço relativo, já com validade e assinatura. Relativo de propósito: quem sabe o host
    // público do catálogo é quem publica o serviço, não ele.
    public String signedPathFor(final String videoId, final String type) {
        final var expiresAt = this.clock.instant().plus(VALIDITY).getEpochSecond();
        return "/videos/%s/medias/%s?expires=%d&signature=%s"
                .formatted(
                        URLEncoder.encode(videoId, StandardCharsets.UTF_8),
                        URLEncoder.encode(type, StandardCharsets.UTF_8),
                        expiresAt,
                        sign(videoId, type, expiresAt));
    }

    public Instant expiresAt() {
        return this.clock.instant().plus(VALIDITY);
    }

    // Confere o selo. Falso para selo errado, para validade vencida e para assinatura de outro vídeo
    // ou de outro tipo de mídia — trocar o id na URL não serve para assistir outro título.
    public boolean valid(final String videoId, final String type, final String expires, final String signature) {
        if (expires == null || signature == null) {
            return false;
        }
        final long expiresAt;
        try {
            expiresAt = Long.parseLong(expires);
        } catch (final NumberFormatException notANumber) {
            return false;
        }
        if (this.clock.instant().getEpochSecond() > expiresAt) {
            return false;
        }
        // Comparação em tempo constante: comparar com equals abre a porta para descobrir a
        // assinatura byte a byte pelo tempo de resposta.
        return MessageDigest.isEqual(
                sign(videoId, type, expiresAt).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(final String videoId, final String type, final long expiresAt) {
        try {
            final var mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(this.secret, ALGORITHM));
            // Os campos vão separados por um caractere que não aparece em id nem em tipo: sem
            // separador, ("ab", "c") e ("a", "bc") teriam a mesma assinatura.
            final var payload = "%s\n%s\n%d".formatted(videoId, type, expiresAt);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (final Exception impossible) {
            // HmacSHA256 é obrigatório em toda JVM, e a chave é uma sequência de bytes válida.
            throw new IllegalStateException("não foi possível assinar o endereço da mídia", impossible);
        }
    }
}
