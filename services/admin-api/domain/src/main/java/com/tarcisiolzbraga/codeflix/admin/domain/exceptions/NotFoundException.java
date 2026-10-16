package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.List;

public class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_TEMPLATE = "%s with ID %s was not found";
    private static final String MEDIA_MESSAGE_TEMPLATE = "Media %s of Video with ID %s was not found";

    private NotFoundException(final String message) {
        super(message, List.of());
    }

    public static NotFoundException with(final Class<? extends AggregateRoot<?>> aggregate, final Identifier id) {
        return new NotFoundException(MESSAGE_TEMPLATE.formatted(aggregate.getSimpleName(), id.getValue()));
    }

    // A mídia não é agregado e não tem id próprio: quem a identifica é o vídeo mais o tipo do
    // arquivo, e é isso que a mensagem precisa dizer para não parecer que o vídeo sumiu.
    public static NotFoundException withMedia(final String type, final Identifier videoId) {
        return new NotFoundException(MEDIA_MESSAGE_TEMPLATE.formatted(type, videoId.getValue()));
    }
}
