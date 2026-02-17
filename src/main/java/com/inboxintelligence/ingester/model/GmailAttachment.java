package com.inboxintelligence.ingester.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gmail_attachment")
public class GmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------- relations ---------- */

    @Column(name = "message_id", nullable = false)
    private String messageId;   // Gmail messageId

    @Column(name = "thread_id")
    private String threadId;

    /* ---------- attachment metadata ---------- */

    @Column(name = "gmail_attachment_id")
    private String gmailAttachmentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_in_bytes")
    private Long sizeInBytes;

    /* ---------- storage ---------- */

    @Column(name = "storage_path", nullable = false)
    private String storagePath;     // S3 key or blob path

    @Column(name = "storage_provider", nullable = false)
    private String storageProvider = "S3";

    /* ---------- processing ---------- */

    @Column(name = "is_inline", nullable = false)
    private Boolean isInline = false;

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    /* ---------- audit ---------- */

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
