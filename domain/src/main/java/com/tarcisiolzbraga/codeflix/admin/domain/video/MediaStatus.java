package com.tarcisiolzbraga.codeflix.admin.domain.video;

// O andar da codificação da mídia. Nasce PENDING no upload; quem move daqui é o encoder, que ainda
// não existe: por enquanto toda mídia enviada fica pendente.
public enum MediaStatus {
    PENDING,
    PROCESSING,
    COMPLETED
}
