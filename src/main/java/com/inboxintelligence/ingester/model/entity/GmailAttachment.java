package com.inboxintelligence.ingester.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "gmail_attachment",
        indexes = {
                @Index(name = "idx_inbox_attachment", columnList = "fk_gmail_inbox_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Foreign Key Mapping
     * ON DELETE CASCADE handled at DB level
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fk_gmail_inbox_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_gmail_gmail_attachment_inbox")
    )
    private GmailInbox gmailInbox;

    // Gmail metadata
    @Column(name = "gmail_attachment_id", length = 255)
    private String gmailAttachmentId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_in_bytes")
    private Long sizeInBytes;

    // Storage
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "storage_provider", nullable = false, length = 100)
    @Builder.Default
    private String storageProvider = "S3";

    // Processing
    @Column(name = "is_inline", nullable = false)
    @Builder.Default
    private Boolean isInline = false;

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
