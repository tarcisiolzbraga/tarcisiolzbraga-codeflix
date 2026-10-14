package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

// A resposta do cadastro é sempre esta, e sempre igual: e-mail novo, e-mail que já existe no
// provedor ou e-mail que já tem conta recebem a mesma coisa. Qualquer diferença contaria a um
// estranho se um endereço está cadastrado.
//
// Quem mostra texto para a pessoa é o front; aqui a mensagem está em inglês, como o resto da API.
public record SignUpResponse(String message) {

    private static final String MESSAGE = "a validation email is on its way";

    public static SignUpResponse accepted() {
        return new SignUpResponse(MESSAGE);
    }
}
