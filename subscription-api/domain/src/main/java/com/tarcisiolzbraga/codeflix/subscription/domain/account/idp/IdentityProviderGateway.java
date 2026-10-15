package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import java.util.Optional;

// O que o cadastro e a assinatura precisam do provedor de identidade, em linguagem de negócio.
//
// Os métodos dizem **o que a pessoa passa a poder**, não como o Keycloak concede — grupo e id de
// grupo são detalhe de lá, e o id ainda muda a cada reimportação do realm, que é descartável aqui.
// Com `addUserToGroup(UserId, GroupId)`, como na referência, os dois conceitos atravessariam o
// domínio inteiro.
//
// **Dois acessos, e eles são diferentes.** Confundi-los foi um erro de verdade, que só aparecia
// olhando os dois serviços juntos:
//
// - **conta** é "existe nesta aplicação e administra a própria assinatura". Vem na confirmação do
//   cadastro e não é revogado: sem ele a pessoa não alcançaria nem a rota de assinar.
// - **catálogo** é "pode assistir". Vem quando a assinatura passa a valer e **sai quando ela é
//   cancelada**, que é o que impede quem só confirmou um e-mail de assistir sem pagar.
public interface IdentityProviderGateway {

    UserId create(NewUser aNewUser);

    Optional<User> userOfEmail(Email anEmail);

    // Chamado na confirmação do cadastro: até ali o provedor tem a pessoa com o e-mail não conferido.
    void markEmailVerified(UserId anUserId);

    void grantAccountAccess(UserId anUserId);

    void grantCatalogAccess(UserId anUserId);

    void revokeCatalogAccess(UserId anUserId);
}
