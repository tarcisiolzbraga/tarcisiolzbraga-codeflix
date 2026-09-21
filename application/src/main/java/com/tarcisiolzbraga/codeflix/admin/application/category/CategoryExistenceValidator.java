package com.tarcisiolzbraga.codeflix.admin.application.category;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Para os casos de uso que recebem IDs de categoria dentro de outro agregado.
public class CategoryExistenceValidator {

    private static final String MISSING_MESSAGE = "Some categories could not be found: %s";

    private final CategoryGateway categoryGateway;

    public CategoryExistenceValidator(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    public void validate(final Set<CategoryID> ids, final ValidationHandler handler) {
        if (ids.isEmpty()) {
            return;
        }
        final var existingIds = this.categoryGateway.findExistingIds(ids);
        if (existingIds.size() == ids.size()) {
            return;
        }
        handler.append(new ValidationError(MISSING_MESSAGE.formatted(missingIdsOf(ids, existingIds))));
    }

    // Ordenado para a mensagem não depender da ordem de iteração do Set.
    private String missingIdsOf(final Set<CategoryID> ids, final Set<CategoryID> existingIds) {
        return ids.stream()
                .filter(id -> !existingIds.contains(id))
                .map(CategoryID::getValue)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
