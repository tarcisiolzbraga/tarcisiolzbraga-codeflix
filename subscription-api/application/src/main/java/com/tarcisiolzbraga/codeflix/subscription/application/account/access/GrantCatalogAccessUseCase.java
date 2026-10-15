package com.tarcisiolzbraga.codeflix.subscription.application.account.access;

import com.tarcisiolzbraga.codeflix.subscription.application.UnitUseCase;

// A entrada é o id da **conta**, que é o que o evento da assinatura carrega. Quem traduz conta em
// pessoa do provedor de identidade é o caso de uso, lendo a conta: o id da pessoa lá é detalhe de
// autenticação e não precisa viajar no evento.
public abstract class GrantCatalogAccessUseCase extends UnitUseCase<String> {}
