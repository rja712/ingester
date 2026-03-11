package com.inboxintelligence.ingester.domain;

import com.inboxintelligence.ingester.model.StoredEmailContentPaths;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for email content and attachment storage.
 * <p>
 * Each implementation targets a different storage backend (local filesystem, S3, GCS, etc.).
 */
public interface EmailStorageProvider {

    StoredEmailContentPaths storeEmailContent(Long mailboxId, String messageId,
                                               String rawMessage, String textBody, String htmlBody) throws IOException;

    Path storeAttachment(Long mailboxId, String messageId,
                         String fileName, byte[] data) throws IOException;

    String providerName();
}
