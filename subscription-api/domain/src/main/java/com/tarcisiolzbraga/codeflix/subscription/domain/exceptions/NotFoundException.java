package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;

// Agregado que não existe é 404, não 422: a requisição está bem formada, o recurso é que não está
// lá. A referência usa DomainException também para este caso, e por ela um plano inexistente
// responderia 422, como se o cliente tivesse mandado algo inválido.
//
// Estende DomainException para o handler poder tratar o caso geral numa cláusula só; quem decide o
// status é a ordem das cláusulas, da mais específica para a mais geral.
public class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_TEMPLATE = "%s with id %s was not found";

    private NotFoundException(final String message) {
        super(message);
    }

    // Sem o token na mensagem, de propósito: ele é a credencial do e-mail, e mensagem de erro vai
    // para log e para o corpo da resposta. Dizer que não existe já é tudo o que o cliente precisa.
    // Token válido de quem não tem conta aqui — um service account, por exemplo — não é falha de
    // autenticação: é não ter conta. Não repete o id de quem chamou, que ele já tem.
    public static NotFoundException callerAccount() {
        return new NotFoundException("there is no account for whoever is calling");
    }

    public static NotFoundException callerSubscription() {
        return new NotFoundException("there is no subscription for whoever is calling");
    }

    public static NotFoundException signUpLink() {
        return new NotFoundException("the sign-up link was not found or is no longer valid");
    }

    public static NotFoundException with(final Class<? extends AggregateRoot<?>> aggregate, final Identifier id) {
        return new NotFoundException(MESSAGE_TEMPLATE.formatted(aggregate.getSimpleName(), id.getValue()));
    }
}
