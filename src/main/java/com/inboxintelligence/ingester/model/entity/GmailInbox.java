package com.inboxintelligence.ingester.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "gmail_inbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_inbox_message",
                        columnNames = {"fk_gmail_mailbox_id", "message_id"}
                )
        },
        indexes = {
                @Index(name = "idx_inbox_mailbox", columnList = "fk_gmail_mailbox_id"),
                @Index(name = "idx_inbox_thread", columnList = "thread_id"),
                @Index(name = "idx_inbox_parent", columnList = "parent_message_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_gmail_mailbox_id", nullable = false)
    private Long gmailMailboxId;

    // Vendor identifiers
    @Column(name = "message_id", nullable = false, length = 128)
    private String messageId;

    @Column(name = "thread_id", nullable = false, length = 128)
    private String threadId;

    @Column(name = "parent_message_id", length = 128)
    private String parentMessageId;

    @Lob
    @Column(name = "raw_message")
    private String rawMessage;

    // Content
    @Lob
    private String subject;

    @Column(name = "from_address", length = 255)
    private String fromAddress;

    @Lob
    @Column(name = "to_address")
    private String toAddress;

    @Lob
    @Column(name = "cc_address")
    private String ccAddress;

    @Lob
    private String body;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
