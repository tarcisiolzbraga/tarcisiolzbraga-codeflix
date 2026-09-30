package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

public record VideoEncoderCompleted(String videoId, String type, String checksum, String encodedPath)
        implements VideoEncoderResult {
}
