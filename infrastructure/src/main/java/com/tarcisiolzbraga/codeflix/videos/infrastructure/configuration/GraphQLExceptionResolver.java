package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

// Sem isto, argumento inválido do cliente chega até ele como INTERNAL_ERROR genérico, e a mensagem
// que diz o que é válido fica só no log do servidor: o Spring GraphQL esconde detalhe de exceção
// não resolvida, e com razão — mas erro de entrada não é falha interna, é resposta ao cliente.
//
// Traduz as três exceções que representam entrada ruim. Qualquer outra segue escondida, que é o
// comportamento certo para falha de infraestrutura.
@Component
public class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected List<GraphQLError> resolveToMultipleErrors(
            final Throwable exception, final DataFetchingEnvironment environment) {
        if (exception instanceof DomainException domainException) {
            return domainException.getErrors().stream()
                    .map(error -> badRequest(error.message(), environment))
                    .toList();
        }
        if (exception instanceof IllegalArgumentException) {
            return List.of(badRequest(exception.getMessage(), environment));
        }
        // Valor que não converte para o tipo do argumento é erro de quem chamou, não falha interna:
        // uma data fora do ISO-8601 na mutation de exemplo vinha como INTERNAL_ERROR, e o nome do
        // campo errado ficava só no log do servidor.
        if (exception instanceof BindException bindException) {
            return bindException.getFieldErrors().stream()
                    .map(error -> badRequest(messageOf(error), environment))
                    .toList();
        }
        return null;
    }

    private static GraphQLError badRequest(final String message, final DataFetchingEnvironment environment) {
        return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(message)
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }

    // O binder nomeia o campo como "$.createdAt"; o cliente o conhece como "createdAt".
    private static String messageOf(final FieldError error) {
        final var field = error.getField().replace("$.", "");
        return "'%s' received an invalid value: %s".formatted(field, error.getRejectedValue());
    }
}
