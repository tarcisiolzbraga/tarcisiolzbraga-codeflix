package com.tarcisiolzbraga.codeflix.admin.infrastructure.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

// Preenche o autor de cada revisão do Envers. É o Hibernate que instancia este listener, não o
// Spring, então nada é injetado aqui: a identidade é lida do SecurityContextHolder, que é o mesmo
// thread da requisição que está gravando.
//
// Fica nulo de propósito quando não há requisição autenticada. O retorno do codificador e o relay
// da tabela de saída gravam fora de qualquer token, e inventar um usuário ali seria mentira no
// histórico — nulo diz a verdade: não foi pessoa nenhuma, foi a aplicação.
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(final Object revisionEntity) {
        ((AuditRevision) revisionEntity).setUserId(currentUserId());
    }

    // O nome do Authentication é o `sub` do token: é o KeycloakJwtConverter que o põe ali ao
    // converter. O sub é o identificador estável do Keycloak, diferente do preferred_username.
    //
    // O anônimo é descartado junto com o ausente: o filtro de anônimo do Spring entrega um
    // Authentication autenticado, chamado "anonymousUser", que não é usuário nenhum.
    private String currentUserId() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
