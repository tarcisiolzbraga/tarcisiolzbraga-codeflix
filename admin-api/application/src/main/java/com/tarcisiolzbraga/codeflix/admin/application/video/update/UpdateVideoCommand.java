package com.tarcisiolzbraga.codeflix.admin.application.video.update;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;

// Sem os estados: publicar, abrir e ativar têm caso de uso próprio.
public record UpdateVideoCommand(String id, VideoFields fields, VideoReferenceIds references) {

    public static UpdateVideoCommand with(
            final String id, final VideoFields fields, final VideoReferenceIds references) {
        return new UpdateVideoCommand(id, fields, references);
    }
}
