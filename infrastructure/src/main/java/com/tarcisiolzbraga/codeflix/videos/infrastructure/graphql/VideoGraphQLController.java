package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import com.tarcisiolzbraga.codeflix.videos.application.video.get.GetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.GqlVideo;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.GqlVideoPage;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.GqlVideoQuery;
import java.util.Objects;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.security.Roles;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;

// As consultas do vídeo. As três relações são resolvidas no VideoRelationsGraphQLController, que é
// uma classe à parte: juntas aqui, o construtor teria cinco dependências, acima do limite de quatro
// parâmetros do projeto — e separá-las também separa o que elas são, consulta e resolução de campo.
@Controller
public class VideoGraphQLController {

    private final ListVideosUseCase listVideosUseCase;
    private final GetVideoUseCase getVideoUseCase;

    public VideoGraphQLController(
            final ListVideosUseCase listVideosUseCase, final GetVideoUseCase getVideoUseCase) {
        this.listVideosUseCase = Objects.requireNonNull(listVideosUseCase, "'listVideosUseCase' should not be null");
        this.getVideoUseCase = Objects.requireNonNull(getVideoUseCase, "'getVideoUseCase' should not be null");
    }

    @QueryMapping
    @Secured({Roles.SUBSCRIBER, Roles.ADMIN})
    public GqlVideoPage videos(@Arguments final GqlVideoQuery query) {
        return GqlVideoPage.from(this.listVideosUseCase.execute(query.toSearchQuery()));
    }

    // Nulo quando o catálogo não serve o vídeo, o que inclui o que existe no admin mas está inativo
    // ou não publicado. O schema declara o campo como anulável justamente por isso.
    @QueryMapping
    @Secured({Roles.SUBSCRIBER, Roles.ADMIN})
    public GqlVideo video(@Argument final String id) {
        return this.getVideoUseCase.execute(VideoID.from(id)).map(GqlVideo::from).orElse(null);
    }
}
