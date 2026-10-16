package com.tarcisiolzbraga.codeflix.subscription.application;

// Caso de uso sem entrada. Classe abstrata, não interface, pelo mesmo motivo do UseCase: com
// herança simples o compilador impede uma classe de acumular dois casos de uso.
//
// Existe para a varredura das vencidas, que não recebe nada — a data de hoje vem do relógio
// injetado, como em todo lugar que decide por tempo neste serviço.
public abstract class NullaryUseCase<OUT> {

    public abstract OUT execute();
}
