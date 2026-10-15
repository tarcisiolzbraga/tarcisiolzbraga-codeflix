package com.tarcisiolzbraga.codeflix.subscription.domain.pagination;

import java.util.List;
import java.util.function.Function;

// Uma página de resultados, com o bastante para o cliente saber onde está.
//
// Igual à da admin-api, de propósito: é o contrato de listagem do monoropo, e divergir faria um
// front ter de aprender dois formatos. A referência do curso tem esta classe e **não a usa** em
// lugar nenhum — aqui ela entra porque existe listagem de verdade.
//
// `total` é o número de registros que casam com a busca, não o da página: é dele que sai o número de
// páginas, e sem ele o cliente não sabe se há mais.
public record Pagination<T>(int currentPage, int perPage, long total, List<T> items) {

    public Pagination {
        items = List.copyOf(items);
    }

    public <R> Pagination<R> map(final Function<T, R> mapper) {
        return new Pagination<>(
                this.currentPage, this.perPage, this.total, this.items.stream().map(mapper).toList());
    }
}
