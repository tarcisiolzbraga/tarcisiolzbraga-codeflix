package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

public record VideoEncoderCompleted(String videoId, String type, String encodedPath)
        implements VideoEncoderResult {
}
