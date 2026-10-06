package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import java.util.Set;

// O gênero como o cliente do catálogo o vê. Sem active nem datas, como nos outros.
//
// O campo categoryIds NÃO está no schema de propósito: ele existe só para o resolvedor em lote saber
// quais categorias pedir. Como o schema não o declara, nenhum cliente consegue pedi-lo, e os ids
// crus — inclusive de categorias inativas — nunca saem daqui.
public record GqlGenre(String id, String name, Set<CategoryID> categoryIds) {

    public GqlGenre {
        categoryIds = Set.copyOf(categoryIds);
    }

    public static GqlGenre from(final GenreOutput output) {
        return new GqlGenre(output.id(), output.name(), output.categories());
    }
}
