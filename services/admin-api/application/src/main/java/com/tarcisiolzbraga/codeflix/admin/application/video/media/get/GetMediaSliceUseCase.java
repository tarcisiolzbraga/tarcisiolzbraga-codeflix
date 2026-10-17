package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;

// Um trecho da mídia, para o player poder buscar na linha de tempo.
//
// Separado do GetMediaUseCase de propósito: a resposta é outra — 206 com o intervalo servido, e não
// 200 com o arquivo —, e misturar os dois num caso de uso com parâmetro opcional faria a rota decidir
// o status por `if` de campo nulo.
public abstract class GetMediaSliceUseCase extends UseCase<GetMediaSliceCommand, MediaSliceOutput> {
}
