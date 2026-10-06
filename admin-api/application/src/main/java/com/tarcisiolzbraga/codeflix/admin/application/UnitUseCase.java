package com.tarcisiolzbraga.codeflix.admin.application;

public abstract class UnitUseCase<IN> {

    public abstract void execute(IN input);
}
