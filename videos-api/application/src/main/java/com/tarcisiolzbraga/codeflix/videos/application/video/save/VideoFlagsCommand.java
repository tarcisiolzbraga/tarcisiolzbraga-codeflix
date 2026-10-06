package com.tarcisiolzbraga.codeflix.videos.application.video.save;

public record VideoFlagsCommand(boolean opened, boolean published, boolean active) {
}
