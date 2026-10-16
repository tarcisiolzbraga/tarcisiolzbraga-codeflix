package com.tarcisiolzbraga.codeflix.videos.application.video.save;

public record VideoMediasCommand(
        String video, String trailer, String banner, String thumbnail, String thumbnailHalf) {
}
