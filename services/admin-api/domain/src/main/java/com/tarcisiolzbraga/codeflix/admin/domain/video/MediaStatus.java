package com.tarcisiolzbraga.codeflix.admin.domain.video;

// O andar da codificação da mídia. Nasce PENDING no upload; quem move daqui é o codificador.
//
// FAILED é estado **terminal desta tentativa**, e não do vídeo: o arquivo enviado continua onde
// está, e um envio novo do mesmo tipo recomeça em PENDING. Antes de existir, a resposta de erro do
// codificador só virava linha de log, e a mídia ficava em PROCESSING para sempre — indistinguível
// de codificação lenta para quem estava olhando o painel.
public enum MediaStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
