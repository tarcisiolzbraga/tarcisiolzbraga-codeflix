package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

// Um arquivo de mídia, como o admin-codeflix o devolve: o conteúdo, o tipo e o nome com que foi
// enviado.
//
// O conteúdo vem como byte[] porque é assim que a rota do admin responde — ela monta o corpo inteiro
// em memória. O catálogo não melhora isso ao repassar, e o README diz o que isso custa.
// `contentRange` é nulo quando o arquivo veio inteiro, e traz o intervalo servido quando veio um
// trecho — é por ele que a rota sabe responder 200 ou 206, sem refazer a conta que o armazenamento
// já fez.
public record VideoMediaFile(String contentType, String filename, byte[] content, String contentRange) {

    // Cópia defensiva nas duas pontas: byte[] é mutável, e um record não protege o arranjo que
    // recebe. Sem isto, quem chamar pode alterar o conteúdo depois de ele ter sido "guardado".
    public VideoMediaFile {
        content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public byte[] content() {
        return this.content.clone();
    }
}
