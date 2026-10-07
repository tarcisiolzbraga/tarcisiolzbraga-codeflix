package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.time.Instant;
import java.util.UUID;

// O vídeo como a listagem precisa dele: chapado e só com o que a página mostra. Existe para a
// consulta não montar o agregado inteiro — vínculos e mídias — e jogar quase tudo fora.
//
// Fica no domínio porque o gateway é contrato de domínio, e é ele que devolve isto. O ano vem como
// número, e não como Year, porque é assim que a projeção sai da consulta.
public record VideoPreview(
        UUID id, String title, int launchedAt, boolean published, boolean active, Instant createdAt) {
}
