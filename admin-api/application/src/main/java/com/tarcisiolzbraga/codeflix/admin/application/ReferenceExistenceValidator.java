package com.tarcisiolzbraga.codeflix.admin.application;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

// Para os casos de uso que recebem IDs de outro agregado: pergunta ao gateway dele quais existem e
// acumula os que faltam. O resultado é limitado pelos IDs recebidos, então não é listagem.
public class ReferenceExistenceValidator<ID extends Identifier> {

    private static final String MISSING_MESSAGE = "Some %s could not be found: %s";

    private final String references;
    private final UnaryOperator<Set<ID>> findExistingIds;

    public ReferenceExistenceValidator(final String references, final UnaryOperator<Set<ID>> findExistingIds) {
        this.references = Objects.requireNonNull(references, "'references' should not be null");
        this.findExistingIds = Objects.requireNonNull(findExistingIds, "'findExistingIds' should not be null");
    }

    public void validate(final Set<ID> ids, final ValidationHandler handler) {
        if (ids.isEmpty()) {
            return;
        }
        final var existingIds = this.findExistingIds.apply(ids);
        if (existingIds.size() == ids.size()) {
            return;
        }
        handler.append(new ValidationError(MISSING_MESSAGE.formatted(this.references, missingIdsOf(ids, existingIds))));
    }

    // Ordenado para a mensagem não depender da ordem de iteração do Set.
    private String missingIdsOf(final Set<ID> ids, final Set<ID> existingIds) {
        return ids.stream()
                .filter(id -> !existingIds.contains(id))
                .map(Identifier::getValue)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
