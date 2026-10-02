package com.tarcisiolzbraga.codeflix.videos.application.category.delete;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import java.util.Objects;

// Não lança NotFoundException quando a categoria não está no catálogo: a remoção chega por
// mensagem, que pode ser reentregue, então apagar duas vezes tem de dar no mesmo. O id nulo, por
// outro lado, é recusado em vez de ignorado em silêncio — é mensagem corrompida, não ausência de
// trabalho, e engolir isso esconderia uma réplica que ficou para trás.
public class DefaultDeleteCategoryUseCase extends DeleteCategoryUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultDeleteCategoryUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public void execute(final CategoryID input) {
        Objects.requireNonNull(input, "'input' should not be null");
        this.categoryGateway.deleteById(input);
    }
}
