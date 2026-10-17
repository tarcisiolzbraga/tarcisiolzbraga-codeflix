package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;

// O pedido de um trecho do arquivo. O `range` é o cabeçalho HTTP como o cliente o mandou.
public record GetMediaSliceCommand(String videoId, VideoMediaType type, String range) {}
