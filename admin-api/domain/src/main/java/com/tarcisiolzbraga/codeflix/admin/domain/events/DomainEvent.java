package com.tarcisiolzbraga.codeflix.admin.domain.events;

import java.time.Instant;

// Algo que já aconteceu no domínio e que interessa fora dele. Diferente do curso, não estende
// Serializable: o evento sai daqui como JSON, e Serializable traria versionamento binário de graça
// para ninguém usar.
public interface DomainEvent {

    Instant occurredOn();
}
