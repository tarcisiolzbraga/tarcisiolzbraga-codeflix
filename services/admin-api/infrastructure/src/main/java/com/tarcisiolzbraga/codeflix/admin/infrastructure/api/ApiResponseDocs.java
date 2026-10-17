package com.tarcisiolzbraga.codeflix.admin.infrastructure.api;

// Códigos e descrições repetidos na documentação OpenAPI das rotas.
public final class ApiResponseDocs {

    public static final String OK = "200";

    // Só a rota de mídia usa estes dois, e por causa do cabeçalho Range: 206 quando devolve um
    // trecho, 416 quando o trecho pedido não existe no arquivo.
    public static final String PARTIAL_CONTENT = "206";

    public static final String RANGE_NOT_SATISFIABLE = "416";
    public static final String CREATED = "201";
    public static final String NO_CONTENT = "204";
    public static final String NOT_FOUND = "404";
    public static final String CONFLICT = "409";
    public static final String UNPROCESSABLE = "422";

    public static final String NOT_FOUND_DESCRIPTION = "Nenhum registro com o id informado";
    public static final String CONFLICT_DESCRIPTION = "Registro em uso por outro; desative em vez de remover";
    public static final String UNPROCESSABLE_DESCRIPTION = "Dados inválidos; errors traz todas as violações";
    public static final String ID_DESCRIPTION = "Id do registro";

    private ApiResponseDocs() {
    }
}
