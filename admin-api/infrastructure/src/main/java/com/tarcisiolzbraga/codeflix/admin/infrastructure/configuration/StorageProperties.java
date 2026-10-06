package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// O armazenamento das mídias. Sem valor padrão: variável faltando derruba a subida, como no banco.
//
// E é a conferência abaixo que faz isso valer de fato: todos os campos são String, então sem ela a
// aplicação subiria com o bucket chamado literalmente "${STORAGE_BUCKET}" e só falharia no primeiro
// envio de arquivo.
@ConfigurationProperties("storage")
public record StorageProperties(
        String bucket, String endpoint, String region, String accessKey, String secretKey) {

    public StorageProperties {
        ConfiguredValue.text("storage.bucket", bucket);
        ConfiguredValue.url("storage.endpoint", endpoint);
        ConfiguredValue.text("storage.region", region);
        ConfiguredValue.text("storage.access-key", accessKey);
        ConfiguredValue.text("storage.secret-key", secretKey);
    }
}
