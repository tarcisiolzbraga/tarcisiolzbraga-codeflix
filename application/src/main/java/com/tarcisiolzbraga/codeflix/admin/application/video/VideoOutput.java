package com.tarcisiolzbraga.codeflix.admin.application.video;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

// Os campos e as referências reaproveitam os mesmos records da entrada, que é a forma do vídeo na
// borda: ano em número, classificação e ids em texto.
public record VideoOutput(
        String id,
        VideoFields fields,
        VideoReferenceIds references,
        boolean opened,
        boolean published,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static VideoOutput from(final Video video) {
        return new VideoOutput(
                video.getId().getValue(),
                fieldsOf(video),
                referencesOf(video),
                video.isOpened(),
                video.isPublished(),
                video.isActive(),
                video.getCreatedAt(),
                video.getUpdatedAt());
    }

    private static VideoFields fieldsOf(final Video video) {
        return new VideoFields(
                video.getTitle(),
                video.getDescription(),
                video.getLaunchedAt() == null ? null : video.getLaunchedAt().getValue(),
                video.getDuration(),
                video.getRating() == null ? null : video.getRating().getLabel());
    }

    private static VideoReferenceIds referencesOf(final Video video) {
        return new VideoReferenceIds(
                valuesOf(video.getCategories()), valuesOf(video.getGenres()), valuesOf(video.getCastMembers()));
    }

    private static Set<String> valuesOf(final Set<? extends Identifier> ids) {
        return ids.stream().map(Identifier::getValue).collect(Collectors.toUnmodifiableSet());
    }
}
