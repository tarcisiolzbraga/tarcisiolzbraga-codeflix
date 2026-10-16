package com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge;

// O userId vem do token da requisição.
//
// O `creditCardToken` é **opcional**, e é isso que permite a mesma cobrança servir a dois chamadores:
// a rota manda o token de uso único que o navegador acabou de obter, e o job não manda nada — ele
// cobra pelo meio de pagamento que a conta guardou. Sem token e sem meio guardado, a cobrança é
// recusada dizendo o que falta.
//
// Número e CVV nunca passam por aqui, em nenhum dos dois caminhos.
public record ChargeSubscriptionCommand(String userId, String creditCardToken) {

    public static ChargeSubscriptionCommand withStoredPaymentMethod(final String userId) {
        return new ChargeSubscriptionCommand(userId, null);
    }
}
