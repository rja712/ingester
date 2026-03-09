package com.inboxintelligence.ingester.domain;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.inboxintelligence.ingester.common.AttachmentStorageProperties;
import com.inboxintelligence.ingester.exception.RetryableGmailApiException;
import com.inboxintelligence.ingester.model.entity.EmailAttachment;
import com.inboxintelligence.ingester.model.entity.EmailContent;
import com.inboxintelligence.ingester.model.entity.GmailMailbox;
import com.inboxintelligence.ingester.persistence.service.EmailAttachmentService;
import com.inboxintelligence.ingester.persistence.service.EmailContentService;
import com.inboxintelligence.ingester.persistence.service.GmailMailboxService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.inboxintelligence.ingester.utils.Base64Utils.decodeBase64Bytes;
import static com.inboxintelligence.ingester.utils.Base64Utils.decodeBase64String;

@Service
@Slf4j
@RequiredArgsConstructor
public class GmailSyncService {

    private final GmailClientFactory gmailClientFactory;
    private final GmailMailboxService gmailMailboxService;
    private final EmailContentService emailContentService;
    private final EmailAttachmentService emailAttachmentService;
    private final AttachmentStorageProperties attachmentStorageProperties;

    private final ConcurrentHashMap<String, ReentrantLock> mailboxLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> mailboxMaxHistory = new ConcurrentHashMap<>();


    public void triggerSyncJob(GmailMailbox mailbox, Long eventHistoryId) throws IOException {

        String email = mailbox.getEmailAddress();

        mailboxMaxHistory.merge(email, eventHistoryId, Math::max);

        ReentrantLock lock = mailboxLocks.computeIfAbsent(email, k -> new ReentrantLock());

        if (mailbox.getHistoryId() > eventHistoryId) {
            log.info("Ignoring stale Gmail event");
            return;
        }

        if (!lock.tryLock()) {
            log.info("Sync already running for {}", email);
            return;
        }

        try {
            runSyncLoop(mailbox);
        } finally {
            lock.unlock();
            mailboxLocks.remove(email, lock);
        }
    }


    private void runSyncLoop(GmailMailbox mailbox) throws IOException {

        String email = mailbox.getEmailAddress();

        while (true) {

            long lastSynced = mailbox.getHistoryId();
            long latestEvent = mailboxMaxHistory.getOrDefault(email, lastSynced);

            if (lastSynced >= latestEvent) {
                log.info("No new Gmail events {} {}", email, lastSynced);
                return;
            }

            syncGmailMailbox(mailbox);
        }
    }


    public void syncGmailMailbox(GmailMailbox mailbox) throws IOException {

        log.info("Syncing mailbox {}", mailbox.getEmailAddress());

        Gmail gmail = gmailClientFactory.createUsingRefreshToken(mailbox.getRefreshToken());

        String pageToken = null;
        Long latestHistoryId = null;

        do {

            ListHistoryResponse response = fetchHistory(gmail, mailbox, pageToken);

            if (response == null) {
                log.error("Gmail returned null response");
                return;
            }

            processHistory(gmail, mailbox, response.getHistory());

            pageToken = response.getNextPageToken();
            latestHistoryId = response.getHistoryId().longValue();

        } while (pageToken != null);

        updateMailboxHistory(mailbox, latestHistoryId);
    }


    private ListHistoryResponse fetchHistory(Gmail gmail, GmailMailbox mailbox, String pageToken) throws IOException {

        var request = gmail.users()
                .history()
                .list("me")
                .setStartHistoryId(BigInteger.valueOf(mailbox.getHistoryId()))
                .setPageToken(pageToken);

        return executeHistoryRequest(request);
    }


    @Retry(name = "gmailRetry")
    private ListHistoryResponse executeHistoryRequest(Gmail.Users.History.List request) {

        try {
            return request.execute();
        } catch (GoogleJsonResponseException ex) {

            int status = ex.getStatusCode();

            if (status == 429 || status >= 500) {
                throw new RetryableGmailApiException("Retrying executeHistoryRequest: Received " + status);
            }

            throw new IllegalArgumentException("Non retryable Gmail error", ex);

        } catch (IOException e) {
            throw new RetryableGmailApiException("Retrying executeHistoryRequest: " + e.getMessage());
        }
    }


    private void processHistory(Gmail gmail, GmailMailbox mailbox, List<History> historyList) {

        if (CollectionUtils.isEmpty(historyList)) {
            return;
        }

        for (History history : historyList) {

            if (history == null) {
                continue;
            }

            if (!CollectionUtils.isEmpty(history.getMessagesAdded())) {
                history.getMessagesAdded()
                        .forEach(msg -> processNewMessageAdded(gmail, mailbox, msg));
            }


        }
    }


    private void processNewMessageAdded(Gmail gmail, GmailMailbox mailbox, HistoryMessageAdded historyMessageAdded) {

        if (historyMessageAdded == null || historyMessageAdded.getMessage() == null) {
            return;
        }

        String messageId = historyMessageAdded.getMessage().getId();

        if (emailContentService.existsByGmailMailboxIdAndMessageId(mailbox.getId(), messageId)) {
            return;
        }

        try {

            Message message = fetchMessage(gmail, messageId);

            String subject = getHeader(message, "Subject");
            String from = getHeader(message, "From");
            String to = getHeader(message, "To");
            String cc = getHeader(message, "Cc");

            // Recursively extract body content and attachment parts from the MIME tree
            ExtractedContent extracted = extractContent(message.getPayload());

            log.info("Email received {}: {} <from:{}> <to:{}>", messageId, subject, from, to);

            EmailContent newEmail = EmailContent.builder()
                    .gmailMailboxId(mailbox.getId())
                    .messageId(message.getId())
                    .threadId(message.getThreadId())
                    .parentMessageId(getHeader(message, "In-Reply-To"))
                    .rawMessage(message.toPrettyString())
                    .subject(subject)
                    .fromAddress(from)
                    .toAddress(to)
                    .ccAddress(cc)
                    .body(extracted.textBody)
                    .bodyHtml(extracted.htmlBody)
                    .sentAt(parseInternalDate(message))
                    .receivedAt(parseInternalDate(message))
                    .isProcessed(false)
                    .build();

            EmailContent savedEmail = emailContentService.save(newEmail);

            log.info("Email saved {}: {}", messageId, subject);

            // Process attachments
            processAttachments(gmail, mailbox, savedEmail, messageId, extracted.attachmentParts);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // ──────────────────────────────────────────────────────────────────────
    //  MIME tree walker — handles all email structures:
    //    - Simple text/html (no parts)
    //    - multipart/alternative  (text + html)
    //    - multipart/mixed        (body + attachments)
    //    - multipart/related      (html + inline images)
    //    - Deeply nested combinations of the above
    //    - Calendar invites (text/calendar)
    //    - Forwarded messages (message/rfc822)
    // ──────────────────────────────────────────────────────────────────────

    private ExtractedContent extractContent(MessagePart payload) {

        ExtractedContent result = new ExtractedContent();

        if (payload == null) {
            return result;
        }

        walkMimeParts(payload, result);
        return result;
    }


    private void walkMimeParts(MessagePart part, ExtractedContent result) {

        String mimeType = part.getMimeType();

        if (mimeType == null) {
            return;
        }

        String mimeTypeLower = mimeType.toLowerCase();

        // Multipart container — recurse into children
        if (mimeTypeLower.startsWith("multipart/")) {
            if (part.getParts() != null) {
                for (MessagePart child : part.getParts()) {
                    walkMimeParts(child, result);
                }
            }
            return;
        }

        // Check if this part is an attachment (has filename or Content-Disposition: attachment)
        if (isAttachment(part)) {
            result.attachmentParts.add(part);
            return;
        }

        // Leaf part — extract body content
        switch (mimeTypeLower) {
            case "text/plain" -> {
                if (result.textBody == null && part.getBody() != null) {
                    result.textBody = decodeBase64String(part.getBody().getData());
                }
            }
            case "text/html" -> {
                if (result.htmlBody == null && part.getBody() != null) {
                    result.htmlBody = decodeBase64String(part.getBody().getData());
                }
            }
            default -> {
                // Non-text leaf parts without a filename (e.g., text/calendar inline,
                // image/*, message/rfc822) — treat as attachment
                result.attachmentParts.add(part);
            }
        }
    }


    private boolean isAttachment(MessagePart part) {

        // Has a filename — it's an attachment
        if (StringUtils.hasText(part.getFilename())) {
            return true;
        }

        // Check Content-Disposition header
        if (part.getHeaders() != null) {
            for (MessagePartHeader header : part.getHeaders()) {
                if ("Content-Disposition".equalsIgnoreCase(header.getName())) {
                    String value = header.getValue().toLowerCase();
                    if (value.startsWith("attachment")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    // ──────────────────────────────────────────────────────────────────────
    //  Attachment processing — download from Gmail API and save locally
    // ──────────────────────────────────────────────────────────────────────

    private void processAttachments(Gmail gmail, GmailMailbox mailbox, EmailContent savedEmail,
                                    String messageId, List<MessagePart> attachmentParts) {

        if (CollectionUtils.isEmpty(attachmentParts)) {
            return;
        }

        for (MessagePart part : attachmentParts) {
            try {
                processOneAttachment(gmail, mailbox, savedEmail, messageId, part);
            } catch (Exception e) {
                log.error("Failed to process attachment '{}' for message {}: {}",
                        part.getFilename(), messageId, e.getMessage());
            }
        }
    }


    private void processOneAttachment(Gmail gmail, GmailMailbox mailbox, EmailContent savedEmail,
                                      String messageId, MessagePart part) throws IOException {

        byte[] data = getAttachmentData(gmail, messageId, part);

        if (data == null || data.length == 0) {
            log.warn("Empty attachment data for '{}' in message {}", part.getFilename(), messageId);
            return;
        }

        String fileName = StringUtils.hasText(part.getFilename())
                ? part.getFilename()
                : "unnamed_" + System.currentTimeMillis();

        // Save to local filesystem: {basePath}/{mailboxId}/{messageId}/{fileName}
        Path storagePath = saveToLocalStorage(mailbox.getId(), messageId, fileName, data);

        boolean isInline = isInlinePart(part);

        String attachmentId = (part.getBody() != null) ? part.getBody().getAttachmentId() : null;
        long sizeInBytes = (part.getBody() != null && part.getBody().getSize() != null)
                ? part.getBody().getSize()
                : data.length;

        EmailAttachment attachment = EmailAttachment.builder()
                .emailContent(savedEmail)
                .emailAttachmentId(attachmentId)
                .fileName(fileName)
                .mimeType(part.getMimeType())
                .sizeInBytes(sizeInBytes)
                .storagePath(storagePath.toString())
                .storageProvider("LOCAL")
                .isInline(isInline)
                .isProcessed(false)
                .build();

        emailAttachmentService.save(attachment);

        log.info("Attachment saved: '{}' ({}) for message {}", fileName, part.getMimeType(), messageId);
    }


    private byte[] getAttachmentData(Gmail gmail, String messageId, MessagePart part) throws IOException {

        MessagePartBody body = part.getBody();

        if (body == null) {
            return null;
        }

        // Small attachments have data inline
        if (StringUtils.hasText(body.getData())) {
            return decodeBase64Bytes(body.getData());
        }

        // Large attachments need a separate API call using attachmentId
        if (StringUtils.hasText(body.getAttachmentId())) {
            MessagePartBody attachmentBody = fetchAttachment(gmail, messageId, body.getAttachmentId());
            if (attachmentBody != null && StringUtils.hasText(attachmentBody.getData())) {
                return decodeBase64Bytes(attachmentBody.getData());
            }
        }

        return null;
    }


    private MessagePartBody fetchAttachment(Gmail gmail, String messageId, String attachmentId) throws IOException {

        var request = gmail.users()
                .messages()
                .attachments()
                .get("me", messageId, attachmentId);

        return executeAttachmentRequest(request);
    }


    @Retry(name = "gmailRetry")
    private MessagePartBody executeAttachmentRequest(Gmail.Users.Messages.Attachments.Get request) {

        try {
            return request.execute();
        } catch (GoogleJsonResponseException ex) {

            int status = ex.getStatusCode();

            if (status == 429 || status >= 500) {
                throw new RetryableGmailApiException("Retrying attachment fetch: Received " + status);
            }

            throw new IllegalArgumentException("Non retryable Gmail error fetching attachment", ex);

        } catch (IOException e) {
            throw new RetryableGmailApiException("Retrying attachment fetch: " + e.getMessage());
        }
    }


    private Path saveToLocalStorage(Long mailboxId, String messageId, String fileName, byte[] data)
            throws IOException {

        Path dir = Path.of(attachmentStorageProperties.basePath(), String.valueOf(mailboxId), messageId);
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


    private boolean isInlinePart(MessagePart part) {

        if (part.getHeaders() == null) {
            return false;
        }

        for (MessagePartHeader header : part.getHeaders()) {
            if ("Content-Disposition".equalsIgnoreCase(header.getName())) {
                return header.getValue().toLowerCase().startsWith("inline");
            }
        }

        return false;
    }


    // ──────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────

    private Message fetchMessage(Gmail gmail, String messageId) throws IOException {

        var request = gmail.users()
                .messages()
                .get("me", messageId)
                .setFormat("full");

        return executeMessageRequest(request);
    }


    @Retry(name = "gmailRetry")
    private Message executeMessageRequest(Gmail.Users.Messages.Get request) {

        try {
            return request.execute();
        } catch (IOException e) {
            throw new RetryableGmailApiException("Retrying message fetch: " + e.getMessage());
        }
    }


    private String getHeader(Message message, String name) {

        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return null;
        }

        return message.getPayload()
                .getHeaders()
                .stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(null);
    }


    private LocalDateTime parseInternalDate(Message message) {

        if (message.getInternalDate() == null) {
            return null;
        }

        return Instant.ofEpochMilli(message.getInternalDate())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }


    private void updateMailboxHistory(GmailMailbox mailbox, Long historyId) {

        if (historyId == null) {
            return;
        }

        log.info("Updating historyId {}", historyId);

        mailbox.setHistoryId(historyId);
        gmailMailboxService.save(mailbox);
    }


    /**
     * Holds extracted content from recursive MIME tree traversal.
     */
    private static class ExtractedContent {

        String textBody;
        String htmlBody;
        List<MessagePart> attachmentParts = new ArrayList<>();
    }
}
