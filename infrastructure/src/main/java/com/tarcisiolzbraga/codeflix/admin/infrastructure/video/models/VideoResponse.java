package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoOutput;
import java.time.Instant;
import java.util.List;
import java.util.Set;

// Plano de propósito: é o contrato da API, e quem consome espera os campos no primeiro nível.
public record VideoResponse(
        String id,
        String title,
        String description,
        Integer launchedAt,
        Double duration,
        String rating,
        boolean opened,
        boolean published,
        boolean active,
        List<String> categories,
        List<String> genres,
        List<String> castMembers,
        Instant createdAt,
        Instant updatedAt) {

    // Ordenados para o JSON não depender da ordem de iteração do Set.
    public static VideoResponse from(final VideoOutput output) {
        return new VideoResponse(
                output.id(),
                output.fields().title(),
                output.fields().description(),
                output.fields().launchedAt(),
                output.fields().duration(),
                output.fields().rating(),
                output.opened(),
                output.published(),
                output.active(),
                sorted(output.references().categories()),
                sorted(output.references().genres()),
                sorted(output.references().castMembers()),
                output.createdAt(),
                output.updatedAt());
    }

    private static List<String> sorted(final Set<String> values) {
        return values.stream().sorted().toList();
    }
}
