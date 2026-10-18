package com.tarcisiolzbraga.codeflix.admin.infrastructure.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// O checksum do arquivo enviado. SHA-256, e não MD5: a função fraca é achado do Sonar mesmo quando
// o uso não é criptográfico.
public final class Hashing {

    private static final String ALGORITHM = "SHA-256";

    private Hashing() {
    }

    public static String checksumOf(final byte[] content) {
        return HexFormat.of().formatHex(digest().digest(content));
    }

    // A mesma conta, lendo o arquivo **em blocos**: um upload de 100 MB não precisa estar em memória
    // para ter o seu checksum. O `DigestInputStream` alimenta o digest enquanto o `transferTo`
    // descarta os bytes lidos.
    public static String checksumOf(final InputStream content) throws IOException {
        final var digest = digest();
        try (var hashing = new DigestInputStream(content, digest)) {
            hashing.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("%s is not available".formatted(ALGORITHM), exception);
        }
    }
}
