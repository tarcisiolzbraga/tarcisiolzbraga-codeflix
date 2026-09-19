package com.tarcisiolzbraga.codeflix.admin.domain.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// O MySQL guarda datas em DATETIME(6): truncar já na criação faz o valor em memória
// ser igual ao que volta do banco. O Instant.now() puro tem nanossegundos no Linux.
public final class InstantUtils {

    private InstantUtils() {
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
