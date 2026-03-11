package com.inboxintelligence.ingester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email-storage.s3")
public record S3StorageProperties(
        String bucket,
        String prefix
) {
}
