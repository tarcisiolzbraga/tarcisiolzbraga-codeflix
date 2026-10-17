package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.videos.application.video.get.GetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.security.Roles;
import java.util.Objects;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

// Quem pode assistir.
//
// A regra está aqui, e não espalhada em anotação, porque ela tem **duas** portas e as duas precisam
// ser lidas juntas para fazer sentido:
//
// 1. quem tem o papel do catálogo assiste. Isso não é atalho: o papel é concedido e revogado pelos
//    eventos da assinatura, então exigi-lo é exigir assinatura viva;
// 2. quem **não** tem o papel assiste o que o título disser ser aberto — e aí precisa ao menos estar
//    identificado. Título aberto sem exigir identificação transformaria a rota numa superfície de
//    banda sem limite de taxa, que este serviço não tem.
//
// O `opened` do vídeo deixou de ser campo inerte com isto. Ele é lido do índice do próprio catálogo,
// que é leitura barata: o documento já está lá.
@Component
public class MediaAccess {

    private final GetVideoUseCase getVideoUseCase;

    public MediaAccess(final GetVideoUseCase getVideoUseCase) {
        this.getVideoUseCase = Objects.requireNonNull(getVideoUseCase, "'getVideoUseCase' should not be null");
    }

    // A decisão é um enum, e não um booleano, porque o controller precisa separar **401 de 403**:
    // "não sei quem é" e "sei, e não basta" são respostas diferentes para quem está do outro lado.
    public enum Decision {
        ALLOWED,
        NEEDS_LOGIN,
        FORBIDDEN
    }

    public Decision decide(final String videoId, final Authentication authentication) {
        if (!identified(authentication)) {
            return Decision.NEEDS_LOGIN;
        }
        if (hasAny(authentication, Roles.SUBSCRIBER, Roles.ADMIN)) {
            return Decision.ALLOWED;
        }
        return openTitle(videoId) ? Decision.ALLOWED : Decision.FORBIDDEN;
    }

    // Vídeo que não está no catálogo não é "aberto": é inexistente, e quem decide o 404 é o repasse,
    // depois. Aqui ele só não ganha a porta do título aberto.
    private boolean openTitle(final String videoId) {
        return this.getVideoUseCase
                .execute(VideoID.from(videoId))
                .map(video -> video.flags().opened())
                .orElse(false);
    }

    // **O token anônimo do Spring Security responde `true` a isAuthenticated()**, então conferir só
    // isso aceitaria quem não está logado. Aqui a filtragem é liberada para todas as rotas e a decisão
    // é por método, o que torna esse detalhe decisivo em vez de teórico.
    private static boolean identified(final Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static boolean hasAny(final Authentication authentication, final String... roles) {
        final var granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        for (final var role : roles) {
            if (granted.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
