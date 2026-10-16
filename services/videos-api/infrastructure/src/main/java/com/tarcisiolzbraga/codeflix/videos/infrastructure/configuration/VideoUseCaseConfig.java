package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.application.video.delete.DefaultDeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.delete.DeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.get.DefaultGetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.get.GetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.list.DefaultListVideosUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.DefaultSaveVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.SaveVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VideoUseCaseConfig {

    private final VideoGateway videoGateway;

    public VideoUseCaseConfig(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Bean
    SaveVideoUseCase saveVideoUseCase() {
        return new DefaultSaveVideoUseCase(this.videoGateway);
    }

    @Bean
    DeleteVideoUseCase deleteVideoUseCase() {
        return new DefaultDeleteVideoUseCase(this.videoGateway);
    }

    @Bean
    ListVideosUseCase listVideosUseCase() {
        return new DefaultListVideosUseCase(this.videoGateway);
    }

    @Bean
    GetVideoUseCase getVideoUseCase() {
        return new DefaultGetVideoUseCase(this.videoGateway);
    }
}
