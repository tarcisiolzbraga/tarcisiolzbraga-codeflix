package com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.models;

// O contrato com o serviço de e-mail, que ainda não existe. Enxuto de propósito: para quem mandar,
// como chamar a pessoa, e o link. Nada que obrigue o outro lado a conhecer rota, token ou tabela
// daqui.
//
// O link carrega a credencial de confirmação, então esta fila é tão sensível quanto a caixa de
// entrada de quem se cadastrou — o que é inerente a validar e-mail, e fica dito.
public record SignUpValidationMessage(String to, String firstname, String confirmationUrl) {}
