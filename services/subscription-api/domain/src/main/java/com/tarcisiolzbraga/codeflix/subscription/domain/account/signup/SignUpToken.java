package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.InternalErrorException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// A credencial que vai no e-mail de validação: quem a tem confirma o cadastro.
//
// Duas consequências disso, e são as razões deste tipo existir em vez de uma String solta:
//
// - é **sorteada** por SecureRandom, com 32 bytes. Token adivinhável deixaria qualquer um confirmar
//   o cadastro de outra pessoa, que é justamente o que a validação por e-mail existe para impedir.
// - o banco guarda só o **resumo**. Token é credencial, e credencial guardada em claro transforma
//   um vazamento de leitura do banco em confirmação de cadastro alheio.
public record SignUpToken(String value) implements ValueObject {

    private static final int BYTES = 32;
    private static final String DIGEST = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    public SignUpToken {
        this.assertArgumentNotEmpty(value, "'token' should not be empty");
    }

    public static SignUpToken generate() {
        final var bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        // Sem padding e sem caracteres que precisariam de escape: o token vai dentro de uma URL.
        return new SignUpToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public String hashed() {
        try {
            final var digest = MessageDigest.getInstance(DIGEST);
            return HexFormat.of().formatHex(digest.digest(this.value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw InternalErrorException.with("%s is not available in this runtime".formatted(DIGEST), exception);
        }
    }
}
