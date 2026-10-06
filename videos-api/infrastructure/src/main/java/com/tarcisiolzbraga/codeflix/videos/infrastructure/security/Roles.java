package com.tarcisiolzbraga.codeflix.videos.infrastructure.security;

// Os papéis que o catálogo reconhece, com o prefixo que o @Secured espera — diferente do hasRole,
// ele não acrescenta ROLE_ sozinho.
//
// São dois, e não um por agregado como na referência do curso, porque aqui a granularidade por
// agregado não se sustentaria: as relações saem resolvidas dentro do vídeo, então quem puder ler
// vídeos já vê o nome da categoria, do gênero e do elenco. Um papel por agregado anunciaria uma
// restrição que a API não cumpre.
public final class Roles {

    // Lê o catálogo: é o papel de quem consome, o usuário final.
    public static final String SUBSCRIBER = "ROLE_CODEFLIX_SUBSCRIBER";

    // Abre tudo, inclusive a porta de escrita da mutation de exemplo.
    public static final String ADMIN = "ROLE_CODEFLIX_ADMIN";

    private Roles() {}
}
