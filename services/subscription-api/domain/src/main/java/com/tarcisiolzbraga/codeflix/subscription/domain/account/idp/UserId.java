package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;

// O identificador da pessoa no provedor de identidade. Guarda texto, e não UUID como os ids daqui:
// este id é do Keycloak, não nosso. Ele hoje usa UUID, mas exigir esse formato seria impor a um
// sistema de fora uma regra que não é nossa — e amarraria o domínio a um provedor.
public record UserId(String value) implements ValueObject {

    public UserId {
        this.assertArgumentNotEmpty(value, "'userId' should not be empty");
    }
}
