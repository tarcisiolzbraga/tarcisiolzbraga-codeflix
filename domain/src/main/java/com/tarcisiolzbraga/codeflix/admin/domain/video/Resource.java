package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Arrays;
import java.util.Objects;

// O arquivo em si, a caminho do armazenamento. O conteúdo é copiado na entrada e na saída para o
// value object não mudar por baixo de quem o guardou, e equals compara os bytes, não a referência.
public record Resource(byte[] content, String checksum, String contentType, String name) {

    public Resource {
        Objects.requireNonNull(content, "'content' should not be null");
        Objects.requireNonNull(checksum, "'checksum' should not be null");
        Objects.requireNonNull(contentType, "'contentType' should not be null");
        Objects.requireNonNull(name, "'name' should not be null");
        content = content.clone();
    }

    public static Resource with(
            final byte[] content, final String checksum, final String contentType, final String name) {
        return new Resource(content, checksum, contentType, name);
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
        if (!(other instanceof Resource that)) {
            return false;
        }
        return Arrays.equals(this.content, that.content)
                && Objects.equals(this.checksum, that.checksum)
                && Objects.equals(this.contentType, that.contentType)
                && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(this.checksum, this.contentType, this.name) + Arrays.hashCode(this.content);
    }
}
