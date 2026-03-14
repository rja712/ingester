package com.inboxintelligence.ingester.model.entity;

import com.inboxintelligence.ingester.model.ProcessedStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "email_content",
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
public class EmailContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_gmail_mailbox_id", nullable = false)
    private Long gmailMailboxId;

    @Column(name = "message_id", nullable = false, length = 1024)
    private String messageId;

    @Column(name = "thread_id", nullable = false, length = 1024)
    private String threadId;

    @Column(name = "parent_message_id", length = 1024)
    private String parentMessageId;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(name = "from_address", columnDefinition = "TEXT")
    private String fromAddress;

    @Column(name = "to_address", columnDefinition = "TEXT")
    private String toAddress;

    @Column(name = "cc_address", columnDefinition = "TEXT")
    private String ccAddress;

    @Column(name = "raw_message_path", length = 1024)
    private String rawMessagePath;

    @Column(name = "body_content_path", length = 1024)
    private String bodyContentPath;

    @Column(name = "body_html_content_path", length = 1024)
    private String bodyHtmlContentPath;

    @Column(name = "processed_content_path", length = 1024)
    private String processedContentPath;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processed_status", nullable = false, length = 32)
    @Builder.Default
    private ProcessedStatus processedStatus = ProcessedStatus.RECEIVED;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
