package com.inboxintelligence.ingester.common;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gmail-api")
public record GmailAPIProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String scope,
        String pubsubTopic
) {
}