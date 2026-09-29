package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.storage.S3StorageService;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.storage.StorageService;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    // forcePathStyle porque o endereço do desenvolvimento é um host e uma porta, não um domínio
    // por bucket, que é o que o estilo virtual exigiria. O checksum fica em WHEN_REQUIRED porque o
    // SDK, por padrão, envia o corpo em aws-chunked com trailer de CRC32, que armazenamentos
    // compatíveis com S3 recusam com "Invalid payload signature".
    @Bean
    S3Client s3Client(final StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    StorageService storageService(final S3Client client, final StorageProperties properties) {
        return new S3StorageService(client, properties.bucket());
    }
}
