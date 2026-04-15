package com.inboxintelligence.ingester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rabbitmq.email-event")
public record EmailEventPublishProperties(String exchange, String routingKey) {
}
