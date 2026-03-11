package com.inboxintelligence.ingester.domain;

import com.inboxintelligence.ingester.config.S3StorageProperties;
import com.inboxintelligence.ingester.model.StoredEmailContentPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * S3-backed email storage provider.
 * <p>
 * TODO: Implement using AWS S3 SDK. Currently throws {@link UnsupportedOperationException}.
 */
@Component
@RequiredArgsConstructor
public class S3EmailStorageProvider implements EmailStorageProvider {

    private final S3StorageProperties s3StorageProperties;

    @Override
    public StoredEmailContentPaths storeEmailContent(Long mailboxId, String messageId,
                                                      String rawMessage, String textBody, String htmlBody)
            throws IOException {
        // TODO: implement S3 upload using s3StorageProperties.bucket() and s3StorageProperties.prefix()
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    @Override
    public Path storeAttachment(Long mailboxId, String messageId,
                                String fileName, byte[] data) throws IOException {
        // TODO: implement S3 upload
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    @Override
    public String providerName() {
        return "S3";
    }
}
