package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.VideoClient;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// As rotas REST do catálogo — as únicas do serviço, e elas existem para **servir mídia a quem
// assina**. Até aqui o acervo publicava os dados do título e nada mais: os campos de mídia guardam a
// localização no armazenamento, não uma URL que o browser alcance, e a rota que devolvia o arquivo
// era a da admin-api, que exige papel de administração.
//
// São duas, e a divisão é o ponto do desenho:
//
// - **`/link` emite um endereço assinado**, depois de conferir quem está pedindo;
// - **a rota da mídia aceita a assinatura** — porque `<video src>` não carrega cabeçalho de
//   Authorization. O browser busca o `src` sozinho, sem token.
//
// A alternativa era o que o portal fazia antes: baixar o arquivo inteiro por `fetch` e tocar de um
// endereço de objeto local. Funcionava, e custava reprodução progressiva e busca na linha de tempo.
//
// A assinatura **não é um token**: ela não concede nada além de um vídeo, de um tipo de mídia, por
// cinco minutos. E a autorização continua num lugar só, porque quem a emite é a rota de cima.
//
// O repasse continua sendo repasse: o arquivo vem da admin-api pelo service account do catálogo,
// que não precisa de credencial nova, e o catálogo segue sem cliente de armazenamento.
@RestController
@RequestMapping("/videos")
public class VideoMediaController {

    private final VideoClient videoClient;

    private final MediaAccess access;

    private final MediaLinks links;

    public VideoMediaController(final VideoClient videoClient, final MediaAccess access, final MediaLinks links) {
        this.videoClient = Objects.requireNonNull(videoClient, "'videoClient' should not be null");
        this.access = Objects.requireNonNull(access, "'access' should not be null");
        this.links = Objects.requireNonNull(links, "'links' should not be null");
    }

    // Emitido no clique, e não junto da consulta do título: assim a assinatura nunca chega vencida a
    // quem leu a sinopse por dez minutos, e não entra em resposta guardada em cache.
    @GetMapping("/{id}/medias/{type}/link")
    public ResponseEntity<MediaLinkResponse> link(
            @PathVariable("id") final String id,
            @PathVariable("type") final String type,
            final Authentication authentication) {
        final var decision = this.access.decide(id, authentication);
        if (decision != MediaAccess.Decision.ALLOWED) {
            return ResponseEntity.status(statusOf(decision)).build();
        }
        return ResponseEntity.ok(new MediaLinkResponse(this.links.signedPathFor(id, type), this.links.expiresAt()));
    }

    // Duas portas, e nenhuma anotação de segurança: a assinatura é uma delas, e `@Secured` recusaria
    // a requisição sem token antes de o selo ser conferido. A regra está no código, logo abaixo, e os
    // seis casos têm teste de ponta a ponta.
    @GetMapping("/{id}/medias/{type}")
    public ResponseEntity<byte[]> media(
            @PathVariable("id") final String id,
            @PathVariable("type") final String type,
            @RequestParam(name = "expires", required = false) final String expires,
            @RequestParam(name = "signature", required = false) final String signature,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) final String range,
            final Authentication authentication) {
        if (signature != null) {
            // Assinatura apresentada e inválida é 403, e não 401: quem a apresentou tinha direito
            // quando a pediu — ela venceu, ou foi mexida.
            return this.links.valid(id, type, expires, signature)
                    ? serve(id, type, range)
                    : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        final var decision = this.access.decide(id, authentication);
        return decision == MediaAccess.Decision.ALLOWED
                ? serve(id, type, range)
                : ResponseEntity.status(statusOf(decision)).build();
    }

    // O status sai do que o admin devolveu: com `Content-Range` é 206, sem ele é 200. Decidir pela
    // presença do cabeçalho de **pedido** seria adivinhar — o admin pode ignorar um Range que não
    // souber atender, e aí 206 com o arquivo inteiro enganaria o player.
    private ResponseEntity<byte[]> serve(final String id, final String type, final String range) {
        return this.videoClient
                .mediaOf(id, type, range)
                .map(file -> ResponseEntity.status(
                                file.contentRange() == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaType.parseMediaType(file.contentType()))
                        // `Accept-Ranges` é o que faz o player saber que pode pedir trecho; sem ele o
                        // browser baixa tudo mesmo que a rota saiba servir partes.
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        // O tamanho que o admin informou, repassado: sem ele a resposta sai em
                        // chunks e o player não sabe o tamanho do que está baixando.
                        .contentLength(file.length())
                        .headers(headers -> {
                            if (file.contentRange() != null) {
                                headers.set(HttpHeaders.CONTENT_RANGE, file.contentRange());
                            }
                        })
                        // inline, e não attachment: o player aponta para esta rota, e attachment faria
                        // o browser baixar em vez de reproduzir.
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"%s\"".formatted(file.filename()))
                        .body(file.content()))
                // Vídeo sem aquela mídia é 404 limpo: existe no acervo e não tem o que reproduzir,
                // que é o caso de um título cadastrado sem upload.
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static HttpStatus statusOf(final MediaAccess.Decision decision) {
        return decision == MediaAccess.Decision.NEEDS_LOGIN ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
    }
}
