package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.api;

import java.time.Instant;

// O endereço assinado da mídia e quando ele deixa de valer.
//
// O caminho é **relativo**: quem sabe o host público do catálogo é quem publica o serviço, não ele —
// atrás de proxy, o host que a requisição traz não é o que o browser usou. O cliente junta com a
// própria base, que ele já conhece para falar com a API.
//
// `expiresAt` vem porque o cliente precisa saber se ainda vale antes de tentar: sem isso, a única
// forma de descobrir é o player falhar na frente de quem está assistindo.
public record MediaLinkResponse(String path, Instant expiresAt) {}
