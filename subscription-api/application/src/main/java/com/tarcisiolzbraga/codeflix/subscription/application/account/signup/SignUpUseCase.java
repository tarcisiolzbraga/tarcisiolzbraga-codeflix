package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.application.UnitUseCase;

// Não devolve nada, e isso é decisão de segurança: a resposta do cadastro é a mesma se o e-mail é
// novo, se já existe no provedor ou se já tem conta. Devolver qualquer coisa que distinga os casos
// diria a um estranho se um e-mail está cadastrado.
public abstract class SignUpUseCase extends UnitUseCase<SignUpCommand> {}
