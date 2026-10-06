package com.tarcisiolzbraga.codeflix.videos.domain.video;

// Os três estados que o admin-codeflix publica.
//
// O catálogo só serve o vídeo quando active e published: não publicado é, por definição, o que ainda
// não deveria estar visível. O opened é replicado e exposto, mas nunca filtra — ele diz se o vídeo é
// aberto a todos ou restrito a assinante, que é questão de acesso, não de o item pertencer ao
// catálogo, e quem decide isso é a camada de autenticação.
public record VideoFlags(boolean opened, boolean published, boolean active) {

    public boolean isVisibleInTheCatalog() {
        return this.active && this.published;
    }
}
