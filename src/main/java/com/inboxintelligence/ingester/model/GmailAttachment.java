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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fk_gmail_inbox_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_gmail_gmail_attachment_inbox")
    )
    private GmailInbox gmailInbox;

    /* ---------- gmail metadata ---------- */

    @Column(name = "gmail_attachment_id", length = 255)
    private String gmailAttachmentId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_in_bytes")
    private Long sizeInBytes;

    /* ---------- storage ---------- */

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "storage_provider", nullable = false, length = 100)
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
