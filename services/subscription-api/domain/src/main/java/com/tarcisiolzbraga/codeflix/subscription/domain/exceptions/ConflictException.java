package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;

// Escrita que perdeu a corrida: alguém gravou o agregado entre a leitura e a gravação, e a cláusula
// do lock otimista não casou com nenhuma linha. É 409, não 404 — a linha existe, só não está na
// versão que quem escreve viu — e não é 422, porque o que o cliente mandou estava correto.
//
// A referência lança IllegalArgumentException nesse caso, que chegaria ao Spring como 500 e faria
// uma disputa de escrita parecer defeito do servidor.
public class ConflictException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_TEMPLATE = "%s with id %s was changed by someone else; read it again";

    private ConflictException(final String message) {
        super(message);
    }

    // O e-mail é por onde a pessoa entra, então dois cadastros com o mesmo não coexistem. Quem chama
    // precisa distinguir isto de erro interno: é o que leva o cadastro a só mandar validar, em vez
    // de tentar criar de novo.
    public static ConflictException emailAlreadyTaken(final String email) {
        return new ConflictException("the email %s is already taken".formatted(email));
    }

    // Clicar no link de validação duas vezes não cria duas contas. É conflito, e não erro de
    // validação: o pedido estava certo, o estado é que não permite mais.
    public static ConflictException signUpAlreadyConfirmed() {
        return new ConflictException("this sign-up was already confirmed");
    }

    // Uma conta assina uma vez por vez. Cancelada não conta: reassinar é legítimo.
    public static ConflictException alreadySubscribed() {
        return new ConflictException("this account already has a subscription");
    }

    public static ConflictException staleVersion(
            final Class<? extends AggregateRoot<?>> aggregate, final Identifier id) {
        return new ConflictException(MESSAGE_TEMPLATE.formatted(aggregate.getSimpleName(), id.getValue()));
    }
}
