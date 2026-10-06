package com.tarcisiolzbraga.codeflix.videos.application.video.save;

import java.util.Set;

public record VideoReferencesCommand(
        Set<String> categories, Set<String> genres, Set<String> castMembers) {
}
