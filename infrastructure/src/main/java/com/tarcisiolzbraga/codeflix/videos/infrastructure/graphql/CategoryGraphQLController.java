package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import com.tarcisiolzbraga.codeflix.videos.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategoryPage;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategoryQuery;
import java.util.Objects;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class CategoryGraphQLController {

    private final ListCategoriesUseCase listCategoriesUseCase;

    public CategoryGraphQLController(final ListCategoriesUseCase listCategoriesUseCase) {
        this.listCategoriesUseCase =
                Objects.requireNonNull(listCategoriesUseCase, "'listCategoriesUseCase' should not be null");
    }

    // @Arguments agrupa os cinco argumentos nomeados do schema num record só, então o método fica
    // com um parâmetro. O cliente continua vendo argumentos com valor padrão.
    @QueryMapping
    public GqlCategoryPage categories(@Arguments final GqlCategoryQuery query) {
        return GqlCategoryPage.from(this.listCategoriesUseCase.execute(query.toSearchQuery()));
    }

}
