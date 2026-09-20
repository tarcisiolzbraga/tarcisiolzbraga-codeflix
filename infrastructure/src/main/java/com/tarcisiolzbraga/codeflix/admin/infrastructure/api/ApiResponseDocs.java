package com.tarcisiolzbraga.codeflix.admin.infrastructure.api;

// Códigos e descrições repetidos na documentação OpenAPI das rotas.
public final class ApiResponseDocs {

    public static final String OK = "200";
    public static final String CREATED = "201";
    public static final String NO_CONTENT = "204";
    public static final String NOT_FOUND = "404";
    public static final String UNPROCESSABLE = "422";

    public static final String NOT_FOUND_DESCRIPTION = "Nenhum registro com o id informado";
    public static final String UNPROCESSABLE_DESCRIPTION = "Dados inválidos; errors traz todas as violações";
    public static final String ID_DESCRIPTION = "Id do registro";

    private ApiResponseDocs() {
    }
}
