package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// Deixa a rota receber o tipo já convertido, sem diferenciar maiúsculas. Tipo desconhecido não
// converte e vira 400, que é o que ele é: um trecho de caminho inválido, não um recurso ausente.
@Component
public class VideoMediaTypeConverter implements Converter<String, VideoMediaType> {

    @Override
    public VideoMediaType convert(final String source) {
        return VideoMediaType.of(source).orElse(null);
    }
}
