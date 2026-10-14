package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

// O corpo do cadastro. A senha vem aqui e vai direto ao provedor de identidade; nada dela é gravado
// neste serviço.
public record SignUpRequest(
        String firstname,
        String lastname,
        String email,
        String password,
        String documentNumber,
        String documentType) {}
