package com.tarcisiolzbraga.codeflix.videos.application.category.get;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import java.util.List;
import java.util.Set;

// Devolve List, e não Pagination, porque o resultado é limitado pelos ids recebidos: resolve as
// referências que o chamador já tem em mão, não é listagem.
public abstract class GetCategoriesByIdUseCase extends UseCase<Set<CategoryID>, List<CategoryOutput>> {
}
