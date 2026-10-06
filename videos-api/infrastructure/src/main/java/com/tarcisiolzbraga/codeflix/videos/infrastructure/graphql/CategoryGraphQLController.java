package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import com.tarcisiolzbraga.codeflix.videos.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategory;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategoryInput;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategoryPage;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategoryQuery;
import java.util.Objects;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.security.Roles;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;

@Controller
public class CategoryGraphQLController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final SaveCategoryUseCase saveCategoryUseCase;

    public CategoryGraphQLController(
            final ListCategoriesUseCase listCategoriesUseCase, final SaveCategoryUseCase saveCategoryUseCase) {
        this.listCategoriesUseCase =
                Objects.requireNonNull(listCategoriesUseCase, "'listCategoriesUseCase' should not be null");
        this.saveCategoryUseCase =
                Objects.requireNonNull(saveCategoryUseCase, "'saveCategoryUseCase' should not be null");
    }

    // @Arguments agrupa os cinco argumentos nomeados do schema num record só, então o método fica
    // com um parâmetro. O cliente continua vendo argumentos com valor padrão.
    @QueryMapping
    @Secured({Roles.SUBSCRIBER, Roles.ADMIN})
    public GqlCategoryPage categories(@Arguments final GqlCategoryQuery query) {
        return GqlCategoryPage.from(this.listCategoriesUseCase.execute(query.toSearchQuery()));
    }

    // EXEMPLO, não o caminho normal do dado: este catálogo replica o que o admin-codeflix publica, e
    // quem grava de verdade é o listener do CDC. Está aqui para acompanhar o módulo do curso e para
    // semear uma categoria à mão sem levantar Kafka e Debezium.
    // Administrador só: ler o catálogo é de assinante, gravar na réplica não é. Um assinante que
    // pudesse chamar isto inventaria categoria que não existe no admin, ou sobrescreveria uma com
    // dado errado, e o próximo evento do CDC corrigiria ou não, em silêncio.
    @MutationMapping
    @Secured(Roles.ADMIN)
    public GqlCategory saveCategory(@Argument final GqlCategoryInput input) {
        return GqlCategory.from(this.saveCategoryUseCase.execute(input.toCommand()), input);
    }
}
