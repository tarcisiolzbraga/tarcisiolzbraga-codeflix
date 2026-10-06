package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.admin.application.video.media.get.GetMediaUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.upload.UploadMediaUseCase;

// Os casos de uso dos arquivos do vídeo, separados dos dados pelo mesmo motivo dos outros grupos:
// manter o construtor do controller dentro do limite de parâmetros.
public record VideoMediaUseCases(UploadMediaUseCase upload, GetMediaUseCase getMedia) {
}
