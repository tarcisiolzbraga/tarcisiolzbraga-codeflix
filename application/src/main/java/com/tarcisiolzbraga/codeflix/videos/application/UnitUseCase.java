package com.tarcisiolzbraga.codeflix.videos.application;

public abstract class UnitUseCase<IN> {

    public abstract void execute(IN input);
}
