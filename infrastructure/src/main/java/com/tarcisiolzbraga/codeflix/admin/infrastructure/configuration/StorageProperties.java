package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// O armazenamento das mídias. Sem valor padrão: variável faltando derruba a subida, como no banco.
@ConfigurationProperties("storage")
public record StorageProperties(
        String bucket, String endpoint, String region, String accessKey, String secretKey) {
}
