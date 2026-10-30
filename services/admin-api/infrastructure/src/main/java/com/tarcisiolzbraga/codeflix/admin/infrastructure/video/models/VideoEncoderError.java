package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

// O erro do codificador. Ele é registrado em log **e** move a mídia para FAILED — antes só virava
// log, porque o domínio não tinha estado de falha, e a mídia ficava em PROCESSING para sempre.
//
// `message` é a explicação que o codificador manda (ele envia "tentativas esgotadas: ..."). Ela não
// é persistida: o que o painel precisa é saber que falhou, e a causa vive no log dos dois lados,
// onde há contexto. Guardá-la no agregado pediria coluna nova e uma decisão sobre retenção de texto
// vindo de fora.
public record VideoEncoderError(String videoId, String type, String checksum, String message)
        implements VideoEncoderResult {
}
