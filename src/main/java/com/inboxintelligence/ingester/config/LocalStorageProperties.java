package com.inboxintelligence.ingester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email-storage.local")
public record LocalStorageProperties(
        String basePath
) {
}
