package com.tarcisiolzbraga.codeflix.admin.infrastructure.util;

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
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(ALGORITHM).digest(content));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("%s is not available".formatted(ALGORITHM), exception);
        }
    }
}
