package com.inboxintelligence.ingester.common;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gmail-api")
public record GmailAPIProperties(
        String applicationName,
        String clientId,
        String clientSecret,
        String redirectUri,
        String tokenUrl,
        String scope,
        String pubsubTopic,
        String projectId,
        String pubsubSubscriptionId
) {
}