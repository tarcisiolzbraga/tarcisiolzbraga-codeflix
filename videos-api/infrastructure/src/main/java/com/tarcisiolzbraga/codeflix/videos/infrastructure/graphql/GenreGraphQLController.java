package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.application.category.get.GetCategoriesByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategory;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GqlGenre;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GqlGenrePage;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GqlGenreQuery;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.security.Roles;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;

@Controller
public class GenreGraphQLController {

    private final ListGenresUseCase listGenresUseCase;
    private final GetCategoriesByIdUseCase getCategoriesByIdUseCase;

    public GenreGraphQLController(
            final ListGenresUseCase listGenresUseCase,
            final GetCategoriesByIdUseCase getCategoriesByIdUseCase) {
        this.listGenresUseCase = Objects.requireNonNull(listGenresUseCase, "'listGenresUseCase' should not be null");
        this.getCategoriesByIdUseCase =
                Objects.requireNonNull(getCategoriesByIdUseCase, "'getCategoriesByIdUseCase' should not be null");
    }

    @QueryMapping
    @Secured({Roles.SUBSCRIBER, Roles.ADMIN})
    public GqlGenrePage genres(@Arguments final GqlGenreQuery query) {
        return GqlGenrePage.from(this.listGenresUseCase.execute(query.toSearchQuery()));
    }

    // Em lote: as categorias de toda a página saem numa consulta só. Resolvido por gênero, listar
    // dez gêneros faria onze consultas.
    //
    // É aqui que a regra do catálogo se cumpre sem código próprio: o GetCategoriesByIdUseCase passa
    // pelo findAllById do gateway da categoria, que deixa as inativas de fora. Elas simplesmente não
    // chegam no mapa, e o gênero sai sem elas.
    @BatchMapping(typeName = "Genre", field = "categories")
    public Map<GqlGenre, List<GqlCategory>> categories(final List<GqlGenre> genres) {
        final var requested = genres.stream()
                .flatMap(genre -> genre.categoryIds().stream())
                .collect(Collectors.toUnmodifiableSet());
        final var found = this.getCategoriesByIdUseCase.execute(requested).stream()
                .collect(Collectors.toMap(CategoryOutput::id, GqlCategory::from));
        return genres.stream().collect(Collectors.toMap(Function.identity(), genre -> categoriesOf(genre, found)));
    }

    // Ordenadas pelo nome, como o GenreResponse do admin-codeflix faz: os ids vêm num Set, cuja
    // ordem de iteração não é definida, então sem isto a mesma consulta devolveria as categorias em
    // ordens diferentes entre chamadas.
    private static List<GqlCategory> categoriesOf(final GqlGenre genre, final Map<String, GqlCategory> found) {
        return genre.categoryIds().stream()
                .map(CategoryID::getValue)
                .map(found::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(GqlCategory::name).thenComparing(GqlCategory::id))
                .toList();
    }
}
