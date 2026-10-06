package com.tarcisiolzbraga.codeflix.videos.domain.pagination;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;

// Quanto cabe numa página do catálogo. Paginar sem limitar o tamanho é devolver coleção sem limite
// com um nome melhor: bastava pedir perPage: 10000 para levar o índice inteiro numa resposta, com o
// servidor montando a lista toda em memória antes de responder.
//
// O teto mora aqui, e não repetido nos construtores das três queries, para a regra ter um lugar só.
// E o pedido fora da faixa é recusado, não reduzido ao máximo: quem pedisse mil itens e recebesse
// cem não teria como saber que a página veio cortada, e pediria a página seguinte em cima de uma
// premissa errada.
public final class PageSize {

    public static final int MAX = 100;

    private static final int MIN = 1;

    private PageSize() {}

    public static int checked(final int perPage) {
        if (perPage < MIN || perPage > MAX) {
            throw DomainException.with(
                    new ValidationError("'perPage' should be between %d and %d".formatted(MIN, MAX)));
        }
        return perPage;
    }
}
