package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.delete.DeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.get.GetVideoByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoUseCase;

// Os casos de uso dos dados do vídeo, agrupados: onze dependências soltas passariam do limite de
// parâmetros do construtor, e a separação por natureza mantém o controller legível.
public record VideoUseCases(
        CreateVideoUseCase create,
        GetVideoByIdUseCase getById,
        ListVideosUseCase list,
        UpdateVideoUseCase update,
        DeleteVideoUseCase delete) {
}
