package com.tarcisiolzbraga.codeflix.admin.domain.video;

// Os três estados do vídeo, só para a reconstrução vinda do banco: quem cria usa o newVideo, e quem
// muda usa os métodos de intenção (publish, open, activate).
public record VideoFlags(boolean opened, boolean published, boolean active) {

    public static VideoFlags closedAndUnpublished() {
        return new VideoFlags(false, false, true);
    }
}
