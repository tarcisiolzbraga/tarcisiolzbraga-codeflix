package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.video.ResourceSlice;
import java.util.Arrays;
import java.util.Objects;

// O trecho que a rota devolve, com o `Content-Range` que o armazenamento calculou — é ele que
// permite responder 206 sem a rota refazer a conta do intervalo.
public record MediaSliceOutput(byte[] content, String contentType, String name, String contentRange) {

    public MediaSliceOutput {
        content = content.clone();
    }

    public static MediaSliceOutput from(final ResourceSlice slice) {
        return new MediaSliceOutput(slice.content(), slice.contentType(), slice.name(), slice.contentRange());
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
        if (!(other instanceof MediaSliceOutput that)) {
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
