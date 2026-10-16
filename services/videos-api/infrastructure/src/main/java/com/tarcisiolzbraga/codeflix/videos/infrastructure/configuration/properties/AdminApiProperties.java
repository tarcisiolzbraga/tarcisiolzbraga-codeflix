package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// A API do admin-codeflix, de onde vem o registro completo depois do aviso do CDC.
@ConfigurationProperties("admin-api")
public record AdminApiProperties(String baseUrl, String clientRegistrationId) {
}
