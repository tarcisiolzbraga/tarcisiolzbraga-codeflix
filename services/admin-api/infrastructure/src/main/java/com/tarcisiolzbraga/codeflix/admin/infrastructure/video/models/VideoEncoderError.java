package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

// O erro do codificador só é registrado: mexer no vídeo por causa dele exigiria um estado de falha
// que o domínio ainda não tem.
public record VideoEncoderError(String videoId, String type, String checksum, String message)
        implements VideoEncoderResult {
}
