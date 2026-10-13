package com.tarcisiolzbraga.codeflix.subscription.application;

// Classe abstrata, não interface: com herança simples o compilador impede uma classe de acumular
// dois casos de uso.
//
// Sem o Presenter da referência, que recebe uma função para moldar a saída: aqui a entrada e a saída
// são record e quem converte para o corpo da resposta é o controller, pelo from(...) do record de
// models — a regra dos outros dois serviços do monorepo.
//
// E sem Either: a validação deste serviço recusa no primeiro erro, lançando DomainException, então
// não há notificação acumulada para devolver.
public abstract class UseCase<IN, OUT> {

    public abstract OUT execute(IN input);
}
