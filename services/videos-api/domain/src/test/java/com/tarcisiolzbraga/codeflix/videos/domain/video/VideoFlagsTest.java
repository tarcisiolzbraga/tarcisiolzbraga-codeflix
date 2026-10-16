package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VideoFlagsTest {

    @Test
    void givenActiveAndPublished_whenAskIfVisible_thenSayYes() {
        final var flags = new VideoFlags(false, true, true);

        assertTrue(flags.isVisibleInTheCatalog());
    }

    @Test
    void givenActiveButNotPublished_whenAskIfVisible_thenSayNo() {
        final var flags = new VideoFlags(true, false, true);

        assertFalse(flags.isVisibleInTheCatalog());
    }

    @Test
    void givenPublishedButInactive_whenAskIfVisible_thenSayNo() {
        final var flags = new VideoFlags(true, true, false);

        assertFalse(flags.isVisibleInTheCatalog());
    }

    // O opened não entra na conta: ele diz se o vídeo é aberto a todos ou restrito a assinante, que
    // é questão de acesso, não de o item pertencer ao catálogo.
    @Test
    void givenAClosedVideoThatIsActiveAndPublished_whenAskIfVisible_thenStillSayYes() {
        final var flags = new VideoFlags(false, true, true);

        assertTrue(flags.isVisibleInTheCatalog());
    }
}
