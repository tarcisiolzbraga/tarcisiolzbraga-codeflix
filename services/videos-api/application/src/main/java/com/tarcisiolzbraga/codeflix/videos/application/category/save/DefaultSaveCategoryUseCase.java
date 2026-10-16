package com.tarcisiolzbraga.codeflix.videos.application.category.save;

import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import java.util.Objects;

// Guarda no catálogo a versão que o admin-codeflix publicou. O erro de validação sobe como exceção,
// e não como Either: ninguém digitou estes dados, eles chegaram por mensagem, então um dado inválido
// é falha de integração e quem consome a mensagem decide o que fazer com ela.
public class DefaultSaveCategoryUseCase extends SaveCategoryUseCase {

    private static final ValidationError NULL_ID = new ValidationError("'id' should not be null");

    private final CategoryGateway categoryGateway;

    public DefaultSaveCategoryUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public SaveCategoryOutput execute(final SaveCategoryCommand input) {
        Objects.requireNonNull(input, "'input' should not be null");
        // Sem id não há o que acumular com os outros erros: a mensagem não identifica categoria
        // alguma, e o CategoryID barraria isto como NullPointerException, não como erro de negócio.
        if (input.id() == null) {
            throw DomainException.with(NULL_ID);
        }

        final var category = toCategory(input);
        final var notification = Notification.create();
        category.validate(notification);
        if (notification.hasError()) {
            throw DomainException.with(notification.getErrors());
        }

        return SaveCategoryOutput.from(this.categoryGateway.save(category));
    }

    private static Category toCategory(final SaveCategoryCommand input) {
        return Category.with(
                CategoryID.from(input.id()),
                input.name(),
                input.description(),
                input.active(),
                input.createdAt(),
                input.updatedAt());
    }
}
