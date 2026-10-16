package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import java.math.BigDecimal;

// Sem o campo `active`, de propósito: mudar dados não decide ativação, que tem rotas próprias. Na
// referência do curso este corpo carrega o booleano e escolhe entre ativar e desativar.
public record ChangePlanRequest(String name, String description, String currency, BigDecimal price) {}
