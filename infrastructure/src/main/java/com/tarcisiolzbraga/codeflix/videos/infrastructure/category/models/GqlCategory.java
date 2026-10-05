package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;

// A categoria como o cliente do catálogo a vê. Sem active, createdAt e updatedAt de propósito: eles
// servem à replicação, não a quem consome o catálogo.
//
// A conversão vive aqui, numa fábrica from(...), e não numa classe de presenter: um presenter só
// mudaria o acoplamento de lugar. É isto que mantém tipo da application fora do schema.
public record GqlCategory(String id, String name, String description) {

    public static GqlCategory from(final CategoryOutput output) {
        return new GqlCategory(output.id(), output.name(), output.description());
    }

}
