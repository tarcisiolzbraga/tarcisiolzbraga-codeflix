package com.tarcisiolzbraga.codeflix.admin.application.video.create;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;

public record CreateVideoCommand(VideoFields fields, VideoReferenceIds references) {

    public static CreateVideoCommand with(final VideoFields fields, final VideoReferenceIds references) {
        return new CreateVideoCommand(fields, references);
    }
}
