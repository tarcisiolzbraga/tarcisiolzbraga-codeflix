package com.tarcisiolzbraga.codeflix.subscription.application;

// Caso de uso que não devolve nada. Classe abstrata, não interface, pelo mesmo motivo do UseCase:
// com herança simples o compilador impede uma classe de acumular dois casos de uso.
public abstract class UnitUseCase<IN> {

    public abstract void execute(IN input);
}
