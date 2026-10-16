package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.list.VideoListOutput;
import java.time.Instant;

public record VideoListResponse(
        String id, String title, Integer launchedAt, boolean published, boolean active, Instant createdAt) {

    public static VideoListResponse from(final VideoListOutput output) {
        return new VideoListResponse(
                output.id(), output.title(), output.launchedAt(), output.published(), output.active(),
                output.createdAt());
    }
}
