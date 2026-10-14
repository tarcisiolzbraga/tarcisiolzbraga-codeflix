package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

// Quem leva o pedido de validação até a pessoa. O domínio não sabe se é e-mail, SMS ou carta, nem
// quem entrega: a implementação publica o pedido e um serviço próprio o entrega.
//
// Esta é a fronteira que a decisão de "serviço de e-mail à parte" criou: deste lado não há SMTP,
// template nem endereço de servidor de e-mail.
public interface SignUpNotifier {

    void sendValidation(SignUpValidation aValidation);
}
