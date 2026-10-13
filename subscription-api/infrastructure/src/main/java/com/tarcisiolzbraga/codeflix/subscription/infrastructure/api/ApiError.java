package com.tarcisiolzbraga.codeflix.subscription.infrastructure.api;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;

// Uma mensagem, e não mensagem mais lista de erros como na admin-api: a validação daqui recusa no
// primeiro problema, então nunca há um segundo erro para listar. Um campo `errors` sempre com um
// item só seria contrato prometendo o que este serviço não entrega.
public record ApiError(String message) {

    public static ApiError from(final DomainException exception) {
        return new ApiError(exception.getMessage());
    }
}
