package com.inboxintelligence.ingester.common;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gmail-api")
@Component
public record GmailAPIProperties (
        String clientId,
        String clientSecret,
        String redirectUri,
        String scope
) {}