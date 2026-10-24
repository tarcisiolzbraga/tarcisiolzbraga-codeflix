package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import java.time.Instant;
import java.util.Optional;

public interface PendingSignUpGateway {

    // A busca é pelo resumo do token, não pelo token: é o resumo que está gravado.
    Optional<PendingSignUp> pendingSignUpOfToken(SignUpToken aToken);

    PendingSignUp save(PendingSignUp aPendingSignUp);

    // Pedir o cadastro de novo é legítimo — o e-mail pode não ter chegado. Isto garante que só o
    // link mais novo vale: os pendentes anteriores do mesmo e-mail saem antes de o novo entrar.
    int deleteUnconfirmedOf(Email anEmail);

    // Apaga o que já não serve a ninguém, e isso é **retenção de dado pessoal**, não arrumação: esta
    // tabela guarda e-mail, nome completo e número de documento de quem **pediu** cadastro, inclusive
    // de quem nunca confirmou e portanto nunca virou cliente. Sem isto, esse dado fica para sempre.
    //
    // O limiar vale para os dois casos, por motivos diferentes:
    //
    // - **não confirmado**: conta a partir do `expires_at`. O link já não funciona desde então, e o
    //   que a janela preserva é o **422** de "o link venceu" para quem clicar num link velho. Apagar
    //   na hora do vencimento faria essa resposta virar 404, e qual das duas o cliente recebe
    //   dependeria de o limpador ter rodado;
    // - **confirmado**: conta a partir do `confirmed_at`. A conta já existe, e a linha só segue viva
    //   para o clique repetido no mesmo link devolver a mesma conta em vez de erro. Passada a janela,
    //   repetir dá 404 — que é a resposta honesta para um link daquela idade.
    int deleteExpiredBefore(Instant aThreshold);
}
