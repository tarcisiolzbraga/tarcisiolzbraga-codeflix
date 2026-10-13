package com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate;

import com.tarcisiolzbraga.codeflix.subscription.application.UseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.PlanOutput;

// A entrada é o id como texto, e não um command de um campo só: não há dado do usuário para
// validar aqui, então um record de uma propriedade seria cerimônia sem conteúdo.
public abstract class DeactivatePlanUseCase extends UseCase<String, PlanOutput> {}
