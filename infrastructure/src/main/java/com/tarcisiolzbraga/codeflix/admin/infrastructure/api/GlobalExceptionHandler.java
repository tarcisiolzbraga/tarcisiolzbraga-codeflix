package com.tarcisiolzbraga.codeflix.admin.infrastructure.api;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Só o que é erro de domínio é traduzido aqui. Falha de infraestrutura continua subindo
// para o tratamento padrão do Spring, que responde 500: nunca 422.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(final NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.from(exception));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(final ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.from(exception));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(final DomainException exception) {
        return ResponseEntity.unprocessableContent().body(ApiError.from(exception));
    }
}
