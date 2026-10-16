package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import java.time.Instant;

// Um arquivo que precisa de codificação acabou de ser enviado. Leva o tipo em vez de um id da
// mídia: o endereço do arquivo é calculado do vídeo e do tipo, então o par já diz de qual mídia
// o codificador está falando, sem a mídia precisar de identidade própria.
//
// O checksum vai junto porque o endereço não distingue um envio do outro: reenviar sobrescreve o
// arquivo no mesmo lugar. É por ele que a resposta atrasada de uma codificação antiga é
// reconhecida e descartada, e é também a chave natural para o codificador não refazer trabalho.
public record VideoMediaCreated(
        String videoId, VideoMediaType type, String filePath, String checksum, Instant occurredOn)
        implements DomainEvent {

    public VideoMediaCreated(
            final String videoId, final VideoMediaType type, final String filePath, final String checksum) {
        this(videoId, type, filePath, checksum, InstantUtils.now());
    }
}
