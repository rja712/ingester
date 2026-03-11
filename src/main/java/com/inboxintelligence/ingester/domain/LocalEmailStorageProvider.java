package com.inboxintelligence.ingester.domain;

import com.inboxintelligence.ingester.config.LocalStorageProperties;
import com.inboxintelligence.ingester.model.StoredEmailContentPaths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores email content and attachments on the local filesystem.
 * <p>
 * Directory layout:
 * <pre>
 *   {basePath}/{mailboxId}/{messageId}/content/   — raw_message.json, body.txt, body.html
 *   {basePath}/{mailboxId}/{messageId}/attachment/ — original attachment files
 * </pre>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LocalEmailStorageProvider implements EmailStorageProvider {

    private final LocalStorageProperties localStorageProperties;

    @Override
    public StoredEmailContentPaths storeEmailContent(Long mailboxId, String messageId,
                                                      String rawMessage, String textBody, String htmlBody)
            throws IOException {

        Path dir = messagePath(mailboxId, messageId).resolve("content");
        Files.createDirectories(dir);

        String rawPath = null;
        String bodyPath = null;
        String htmlPath = null;

        if (rawMessage != null) {
            Path p = dir.resolve("raw_message.json");
            Files.writeString(p, rawMessage, StandardCharsets.UTF_8);
            rawPath = p.toString();
        }

        if (textBody != null) {
            Path p = dir.resolve("body.txt");
            Files.writeString(p, textBody, StandardCharsets.UTF_8);
            bodyPath = p.toString();
        }

        if (htmlBody != null) {
            Path p = dir.resolve("body.html");
            Files.writeString(p, htmlBody, StandardCharsets.UTF_8);
            htmlPath = p.toString();
        }

        log.info("Email content stored locally for message {}", messageId);

        return new StoredEmailContentPaths(rawPath, bodyPath, htmlPath);
    }

    @Override
    public Path storeAttachment(Long mailboxId, String messageId,
                                String fileName, byte[] data) throws IOException {

        Path dir = messagePath(mailboxId, messageId).resolve("attachment");
        Files.createDirectories(dir);

        // Sanitize filename to avoid path traversal
        String safeFileName = Path.of(fileName).getFileName().toString();
        Path filePath = dir.resolve(safeFileName);

        // Handle duplicate filenames
        if (Files.exists(filePath)) {
            String name = safeFileName;
            String ext = "";
            int dotIdx = safeFileName.lastIndexOf('.');
            if (dotIdx > 0) {
                name = safeFileName.substring(0, dotIdx);
                ext = safeFileName.substring(dotIdx);
            }
            int counter = 1;
            while (Files.exists(filePath)) {
                filePath = dir.resolve(name + "_" + counter + ext);
                counter++;
            }
        }

        Files.write(filePath, data);
        return filePath;
    }

    @Override
    public String providerName() {
        return "LOCAL";
    }

    private Path messagePath(Long mailboxId, String messageId) {
        return Path.of(localStorageProperties.basePath(), String.valueOf(mailboxId), messageId);
    }
}
