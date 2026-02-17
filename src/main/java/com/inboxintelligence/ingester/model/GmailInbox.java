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
@Table(
        name = "gmail_inbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_inbox_message",
                        columnNames = {"fk_gmail_mailbox_id", "vendor_message_id"}
                )
        }
)
public class GmailInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------- relations ---------- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fk_gmail_mailbox_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_gmail_inbox_mailbox")
    )
    private GmailMailbox gmailMailbox;

    /* ---------- vendor identifiers ---------- */

    @Column(name = "vendor_message_id", nullable = false, length = 128)
    private String vendorMessageId;

    @Column(name = "vendor_thread_id", nullable = false, length = 128)
    private String vendorThreadId;

    @Column(name = "parent_vendor_message_id", length = 128)
    private String parentVendorMessageId;

    /* ---------- content ---------- */

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "from_address", length = 255)
    private String fromAddress;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    /* ---------- state ---------- */

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /* ---------- audit ---------- */

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /* ---------- lifecycle ---------- */

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
