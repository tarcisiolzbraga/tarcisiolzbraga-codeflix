package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import java.util.Arrays;
import java.util.Objects;

// O que a rota precisa para devolver o arquivo. Copia o conteúdo na entrada e na saída pelo mesmo
// motivo do Resource: quem recebeu não pode alterar o que o caso de uso devolveu.
public record MediaOutput(byte[] content, String contentType, String name) {

    public MediaOutput {
        content = content.clone();
    }

    public static MediaOutput from(final Resource resource) {
        return new MediaOutput(resource.content(), resource.contentType(), resource.name());
    }

    @Override
    public byte[] content() {
        return this.content.clone();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaOutput that)) {
            return false;
        }
        return Arrays.equals(this.content, that.content)
                && Objects.equals(this.contentType, that.contentType)
                && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(this.contentType, this.name) + Arrays.hashCode(this.content);
    }
}
