package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.api;

import com.tarcisiolzbraga.codeflix.admin.application.video.activate.ActivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.close.CloseVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.deactivate.DeactivateVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.open.OpenVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.publish.PublishVideoUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.video.unpublish.UnpublishVideoUseCase;

// Os casos de uso de mudança de estado, um por intenção.
public record VideoStateUseCases(
        PublishVideoUseCase publish,
        UnpublishVideoUseCase unpublish,
        OpenVideoUseCase open,
        CloseVideoUseCase close,
        ActivateVideoUseCase activate,
        DeactivateVideoUseCase deactivate) {
}
