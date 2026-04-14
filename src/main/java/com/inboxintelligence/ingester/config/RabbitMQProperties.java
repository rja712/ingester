package com.inboxintelligence.ingester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rabbitmq.email-processing")
public record RabbitMQProperties(String exchange, String queue, String routingKey) {
}
