package com.tarcisiolzbraga.codeflix.videos.infrastructure.elasticsearch;

import org.springframework.data.elasticsearch.core.query.Criteria;

// O termo de busca é texto livre, e texto livre tem espaço. Um contains com o termo inteiro não
// serve: o Spring Data traduz contains num wildcard `*valor*` e recusa construí-lo quando o valor
// tem espaço dentro — "Cannot constructQuery ... Use expression or multiple clauses instead". A
// listagem inteira caía com erro de infraestrutura, e bastava procurar por "Denis Villeneuve".
//
// Então o termo é quebrado em palavras: cada palavra tem de aparecer em algum dos campos, e todas
// são exigidas. Isso preserva o casamento por pedaço de palavra, que é o que serve a uma busca de
// catálogo — "Reb" acha "Rebecca" —, e que um match por token não daria.
public final class SearchTerms {

    private SearchTerms() {}

    // Cada palavra entra como subcriteria própria: deixar os "ou" dos campos no mesmo nível dos "e"
    // das palavras entregaria o agrupamento à precedência, e "Filmes xpto" voltaria com Filmes.
    public static Criteria across(final String terms, final String... fields) {
        var criteria = new Criteria();
        for (final var word : terms.trim().split("\\s+")) {
            criteria = criteria.subCriteria(inAnyField(word, fields));
        }
        return criteria;
    }

    private static Criteria inAnyField(final String word, final String... fields) {
        Criteria group = null;
        for (final var field : fields) {
            final var contains = new Criteria(field).contains(word);
            group = group == null ? contains : group.or(contains);
        }
        return group;
    }
}
