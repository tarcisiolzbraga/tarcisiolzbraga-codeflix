package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

public record VideoEncoderProcessing(String videoId, String type) implements VideoEncoderResult {
}
