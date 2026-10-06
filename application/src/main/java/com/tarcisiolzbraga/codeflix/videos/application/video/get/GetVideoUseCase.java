package com.tarcisiolzbraga.codeflix.videos.application.video.get;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import java.util.Optional;

// Um vídeo só, pelo id: é o que a página de um título precisa. Devolve Optional, e não lança, porque
// "não existe no catálogo" é resposta normal — inclusive para um vídeo que existe no admin mas está
// inativo ou não publicado, que o gateway esconde.
public abstract class GetVideoUseCase extends UseCase<VideoID, Optional<VideoOutput>> {
}
