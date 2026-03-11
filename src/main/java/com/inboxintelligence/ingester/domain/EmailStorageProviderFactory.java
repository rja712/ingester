package com.inboxintelligence.ingester.domain;

import com.inboxintelligence.ingester.config.EmailStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the active {@link EmailStorageProvider} based on the
 * {@code email-storage.provider} configuration property.
 */
@Component
@Slf4j
public class EmailStorageProviderFactory {

    private final EmailStorageProvider activeProvider;

    public EmailStorageProviderFactory(EmailStorageProperties properties,
                                       List<EmailStorageProvider> providers) {

        Map<String, EmailStorageProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(EmailStorageProvider::providerName, Function.identity()));

        String requested = properties.provider().toUpperCase();
        this.activeProvider = providerMap.get(requested);

        if (this.activeProvider == null) {
            throw new IllegalStateException(
                    "No EmailStorageProvider found for provider '%s'. Available: %s"
                            .formatted(requested, providerMap.keySet()));
        }

        log.info("Email storage provider: {}", requested);
    }

    public EmailStorageProvider getProvider() {
        return activeProvider;
    }
}
