package com.tarcisiolzbraga.codeflix.admin.infrastructure.util;

import org.springframework.data.jpa.domain.Specification;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    public static <T> Specification<T> like(final String property, final String term) {
        return (root, query, builder) ->
                builder.like(builder.upper(root.get(property)), "%" + term.toUpperCase() + "%");
    }
}
