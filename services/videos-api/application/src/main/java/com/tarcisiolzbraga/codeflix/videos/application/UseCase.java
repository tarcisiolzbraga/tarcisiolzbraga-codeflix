package com.tarcisiolzbraga.codeflix.videos.application;

// Classe abstrata, não interface: com herança simples o compilador impede uma classe de acumular
// dois casos de uso, que é a regra "um caso de uso por classe" deixando de depender de disciplina.
public abstract class UseCase<IN, OUT> {

    public abstract OUT execute(IN input);
}
