package com.tarcisiolzbraga.codeflix.subscription.infrastructure.api;

// Códigos e descrições repetidos na documentação OpenAPI das rotas.
public final class ApiResponseDocs {

    public static final String OK = "200";
    public static final String CREATED = "201";
    public static final String NOT_FOUND = "404";
    public static final String CONFLICT = "409";
    public static final String UNPROCESSABLE = "422";
    public static final String TOO_MANY_REQUESTS = "429";

    public static final String NOT_FOUND_DESCRIPTION = "Nenhum registro com o id informado";
    public static final String CONFLICT_DESCRIPTION = "O registro mudou desde a leitura; leia de novo e repita";
    public static final String UNPROCESSABLE_DESCRIPTION = "Dados inválidos; message diz o que foi recusado";
    public static final String ID_DESCRIPTION = "Id do registro, em UUID";
    public static final String TOO_MANY_REQUESTS_DESCRIPTION =
            "Teto de requisições da origem atingido; Retry-After diz quantos segundos esperar";

    private ApiResponseDocs() {}
}
