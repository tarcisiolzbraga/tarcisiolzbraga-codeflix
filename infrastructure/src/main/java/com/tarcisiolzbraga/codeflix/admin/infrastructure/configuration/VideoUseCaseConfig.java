package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.application.video.activate.ActivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.activate.DefaultActivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.close.CloseVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.close.DefaultCloseVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.create.DefaultCreateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.deactivate.DeactivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.deactivate.DefaultDeactivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.delete.DefaultDeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.delete.DeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.get.DefaultGetVideoByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.get.GetVideoByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.list.DefaultListVideosUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.get.DefaultGetMediaUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.get.GetMediaUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.upload.DefaultUploadMediaUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.upload.UploadMediaUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.open.DefaultOpenVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.open.OpenVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.publish.DefaultPublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.publish.PublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.unpublish.DefaultUnpublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.unpublish.UnpublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.DefaultUpdateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api.VideoMediaUseCases;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api.VideoStateUseCases;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api.VideoUseCases;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O component scan só enxerga a infrastructure, então os casos de uso são registrados aqui.
@Configuration
public class VideoUseCaseConfig {

    private final CategoryGateway categoryGateway;
    private final GenreGateway genreGateway;
    private final CastMemberGateway castMemberGateway;
    private final VideoGateway videoGateway;
    private final MediaResourceGateway mediaResourceGateway;

    public VideoUseCaseConfig(
            final CategoryGateway categoryGateway,
            final GenreGateway genreGateway,
            final CastMemberGateway castMemberGateway,
            final VideoGateway videoGateway,
            final MediaResourceGateway mediaResourceGateway) {
        this.categoryGateway = categoryGateway;
        this.genreGateway = genreGateway;
        this.castMemberGateway = castMemberGateway;
        this.videoGateway = videoGateway;
        this.mediaResourceGateway = mediaResourceGateway;
    }

    @Bean
    CreateVideoUseCase createVideoUseCase() {
        return new DefaultCreateVideoUseCase(this.categoryGateway, this.genreGateway, this.castMemberGateway, this.videoGateway);
    }

    @Bean
    GetVideoByIdUseCase getVideoByIdUseCase() {
        return new DefaultGetVideoByIdUseCase(this.videoGateway);
    }

    @Bean
    ListVideosUseCase listVideosUseCase() {
        return new DefaultListVideosUseCase(this.videoGateway);
    }

    @Bean
    UpdateVideoUseCase updateVideoUseCase() {
        return new DefaultUpdateVideoUseCase(this.categoryGateway, this.genreGateway, this.castMemberGateway, this.videoGateway);
    }

    @Bean
    PublishVideoUseCase publishVideoUseCase() {
        return new DefaultPublishVideoUseCase(this.videoGateway);
    }

    @Bean
    UnpublishVideoUseCase unpublishVideoUseCase() {
        return new DefaultUnpublishVideoUseCase(this.videoGateway);
    }

    @Bean
    OpenVideoUseCase openVideoUseCase() {
        return new DefaultOpenVideoUseCase(this.videoGateway);
    }

    @Bean
    CloseVideoUseCase closeVideoUseCase() {
        return new DefaultCloseVideoUseCase(this.videoGateway);
    }

    @Bean
    ActivateVideoUseCase activateVideoUseCase() {
        return new DefaultActivateVideoUseCase(this.videoGateway);
    }

    @Bean
    DeactivateVideoUseCase deactivateVideoUseCase() {
        return new DefaultDeactivateVideoUseCase(this.videoGateway);
    }

    @Bean
    DeleteVideoUseCase deleteVideoUseCase() {
        return new DefaultDeleteVideoUseCase(this.videoGateway, this.mediaResourceGateway);
    }

    // O controller recebe os casos de uso agrupados: onze dependências soltas passariam do limite.
    @Bean
    VideoUseCases videoUseCases() {
        return new VideoUseCases(
                createVideoUseCase(),
                getVideoByIdUseCase(),
                listVideosUseCase(),
                updateVideoUseCase(),
                deleteVideoUseCase());
    }

    @Bean
    UploadMediaUseCase uploadMediaUseCase() {
        return new DefaultUploadMediaUseCase(this.videoGateway, this.mediaResourceGateway);
    }

    @Bean
    GetMediaUseCase getMediaUseCase() {
        return new DefaultGetMediaUseCase(this.mediaResourceGateway);
    }

    @Bean
    VideoStateUseCases videoStateUseCases() {
        return new VideoStateUseCases(
                publishVideoUseCase(),
                unpublishVideoUseCase(),
                openVideoUseCase(),
                closeVideoUseCase(),
                activateVideoUseCase(),
                deactivateVideoUseCase());
    }

    @Bean
    VideoMediaUseCases videoMediaUseCases() {
        return new VideoMediaUseCases(uploadMediaUseCase(), getMediaUseCase());
    }
}
