package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Arrays;
import java.util.Objects;

// Um trecho do arquivo, como o armazenamento o devolve quando se pede um Range.
//
// Existe separado do Resource porque carrega o que só faz sentido num trecho: **o `Content-Range`
// que o armazenamento calculou**, com o intervalo servido e o tamanho total. Recalculá-lo aqui seria
// repetir uma conta que o S3 já fez e já devolveu.
public record ResourceSlice(byte[] content, String contentType, String name, String contentRange) {

    public ResourceSlice {
        Objects.requireNonNull(content, "'content' should not be null");
        Objects.requireNonNull(contentType, "'contentType' should not be null");
        Objects.requireNonNull(name, "'name' should not be null");
        Objects.requireNonNull(contentRange, "'contentRange' should not be null");
        content = content.clone();
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
        if (!(other instanceof ResourceSlice that)) {
            return false;
        }
        return Arrays.equals(this.content, that.content)
                && Objects.equals(this.contentType, that.contentType)
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.contentRange, that.contentRange);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(this.contentType, this.name, this.contentRange) + Arrays.hashCode(this.content);
    }
}
